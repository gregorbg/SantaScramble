package com.suushiemaniac.cubing.bld.santascramble;

import com.suushiemaniac.cubing.alglib.alg.Algorithm;
import com.suushiemaniac.cubing.bld.analyze.BldCube;
import com.suushiemaniac.cubing.bld.analyze.BldCube.CornerParityMethod;
import com.suushiemaniac.cubing.bld.analyze.BldPuzzle;
import com.suushiemaniac.cubing.bld.filter.BldScramble;
import com.suushiemaniac.cubing.bld.filter.condition.BooleanCondition;
import com.suushiemaniac.cubing.bld.filter.condition.IntCondition;
import com.suushiemaniac.cubing.bld.model.enumeration.piece.CubicPieceType;
import com.suushiemaniac.cubing.bld.model.enumeration.piece.PieceType;
import com.suushiemaniac.cubing.bld.model.enumeration.puzzle.CubicPuzzle;
import com.suushiemaniac.io.net.rest.RestFul;
import com.suushiemaniac.io.net.rest.request.Request;
import com.suushiemaniac.io.net.rest.response.Response;
import com.suushiemaniac.lang.json.JSON;
import javafx.animation.FadeTransition;
import javafx.animation.SequentialTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.SpinnerValueFactory.IntegerSpinnerValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;
import javafx.util.Duration;
import javafx.util.Pair;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.function.Function;

public class Main extends Application {
    @Override
    public void start(Stage primaryStage) throws Exception {
        List<CubicPuzzle> choices = new ArrayList<>();
        choices.add(CubicPuzzle.THREE_BLD);
        choices.add(CubicPuzzle.FOUR_BLD);
        choices.add(CubicPuzzle.FIVE_BLD);

        ChoiceDialog<CubicPuzzle> userPick = new ChoiceDialog<>(CubicPuzzle.THREE_BLD, choices);
        userPick.setTitle("SantaScramble");
        userPick.setHeaderText("Which puzzle do you want to generate a scramble for?");
        userPick.setContentText("Please choose:");

		Optional<CubicPuzzle> result = userPick.showAndWait();
		CubicPuzzle choice = null;

		while (result.isPresent() && !result.get().equals(choice)) {
			userPick.hide();

			choice = result.get();
			this.showSantaPanel(choice);

			result = userPick.showAndWait();
        }
    }

    protected void showSantaPanel(CubicPuzzle puzzle) {
		Dialog<BldScramble> santa = new Dialog<>();
		santa.setTitle("SantaScramble - " + puzzle.toString());
		santa.setHeaderText("Please specify what scramble you desire");

		santa.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

		VBox parent = new VBox(12);
		VBox container = new VBox(12);

		List<String> boolProperties = new ArrayList<>(Arrays.asList("Parity", "Buffer"));
		List<String> intProperties = new ArrayList<>(Arrays.asList("Targets", "BreakIns", "PreSolved", "MisOriented"));

		Map<String, Function<PieceType, Integer>> maximumBoundMap = new HashMap<>();
		maximumBoundMap.put("Targets", type -> ((type.getNumPiecesNoBuffer() / 2) * 3) + (type.getNumPiecesNoBuffer() % 2));
		maximumBoundMap.put("BreakIns", type -> type.getNumPiecesNoBuffer() / 2);
		maximumBoundMap.put("PreSolved", PieceType::getNumPieces);
		maximumBoundMap.put("MisOriented", PieceType::getNumPieces);

		Map<PieceType, Map<String, Pair<Spinner<Integer>, Spinner<Integer>>>> masterIntMap = new HashMap<>();
		Map<PieceType, Map<String, Pair<CheckBox, CheckBox>>> masterBoolMap = new HashMap<>();
		Map<PieceType, HBox> masterLetterPairMap = new HashMap<>();

		for (PieceType type : puzzle.getAnalyzingPuzzle().getPieceTypes()) {
			Map<String, Pair<Spinner<Integer>, Spinner<Integer>>> typeIntMap = new HashMap<>();
			Map<String, Pair<CheckBox, CheckBox>> typeBoolMap = new HashMap<>();

			Label heading = new Label(type.humanName());
			heading.setFont(Font.font(heading.getFont().getFamily(), FontWeight.BOLD, heading.getFont().getSize()));
			container.getChildren().add(heading);

			if (puzzle.getAnalyzingPuzzle() instanceof BldCube
					&& type == CubicPieceType.CORNER
					&& puzzle.getAnalyzingPuzzle().getPieceTypes().contains(CubicPieceType.EDGE)) {
				BldCube castCube = (BldCube) puzzle.getAnalyzingPuzzle();

				ObservableList<CornerParityMethod> methodPool = FXCollections.observableArrayList(CornerParityMethod.values());
				ComboBox<CornerParityMethod> methods = new ComboBox<>(methodPool);
				methods.getSelectionModel().select(castCube.getCornerParityMethod());

				methods.setOnAction(event -> castCube.setCornerParityMethod(methods.getSelectionModel().getSelectedItem()));

				Label methodLabel = new Label("Parity method?");
				HBox methodBox = new HBox(36, methodLabel, methods);
				methodBox.setAlignment(Pos.CENTER_LEFT);
				HBox.setHgrow(methodLabel, Priority.ALWAYS);
				HBox.setHgrow(methods, Priority.ALWAYS);

				container.getChildren().add(methodBox);
			}

			for (String boolProperty : boolProperties) {
				CheckBox truth = new CheckBox("No");
				CheckBox matters = new CheckBox("I don't care");
				matters.setSelected(true);

				truth.disableProperty().bind(matters.selectedProperty());
				truth.textProperty().bind(truth.selectedProperty().asString());
				//truth.selectedProperty().addListener((observable, oldValue, newValue) -> truth.setText(newValue ? "Yes" : "No"));

				typeBoolMap.put(boolProperty.toLowerCase(), new Pair<>(truth, matters));

				Label label = new Label(boolProperty + "?");

				HBox line = new HBox(36, label, truth, matters);
				line.setAlignment(Pos.CENTER_LEFT);
				HBox.setHgrow(label, Priority.ALWAYS);
				HBox.setHgrow(truth, Priority.ALWAYS);
				HBox.setHgrow(matters, Priority.ALWAYS);

				if (boolProperty.equals("Buffer")) {
					CheckBox allowMisOrient = new CheckBox("Allow mis-oriented?");
					allowMisOrient.setFont(Font.font(allowMisOrient.getFont().getFamily(), 9));
					allowMisOrient.setSelected(true);
					allowMisOrient.disableProperty().bind(truth.selectedProperty().not().or(matters.selectedProperty()));

					typeBoolMap.put("bufferallow", new Pair<>(new CheckBox(), allowMisOrient));

					line.getChildren().add(allowMisOrient);
					HBox.setHgrow(allowMisOrient, Priority.ALWAYS);
				}

				container.getChildren().add(line);
			}

			for (String intProperty : intProperties) {
				Function<PieceType, Integer> maxBounds = maximumBoundMap.get(intProperty);
				int max = maxBounds.apply(type);

				Spinner<Integer> minSpin = new Spinner<>(0, max, 0, 1);
				minSpin.setMaxWidth(72);
				minSpin.getValueFactory().setWrapAround(false);
				Label minText = new Label("min");
				Spinner<Integer> maxSpin = new Spinner<>(0, max, max, 1);
				maxSpin.setMaxWidth(72);
				maxSpin.getValueFactory().setWrapAround(false);
				Label maxText = new Label("max");

				SpinnerValueFactory<Integer> minFactory = minSpin.getValueFactory();
				SpinnerValueFactory<Integer> maxFactory = maxSpin.getValueFactory();

				if (minFactory instanceof IntegerSpinnerValueFactory
						&& maxFactory instanceof IntegerSpinnerValueFactory) {
					IntegerSpinnerValueFactory intMinFactory = (IntegerSpinnerValueFactory) minFactory;
					IntegerSpinnerValueFactory intMaxFactory = (IntegerSpinnerValueFactory) maxFactory;

					intMaxFactory.minProperty().bind(intMinFactory.valueProperty());
				}

				typeIntMap.put(intProperty.toLowerCase(), new Pair<>(minSpin, maxSpin));

				if (!intProperty.equals("MisOriented") || type.getTargetsPerPiece() > 1) {
					Label label = new Label("# " + intProperty);

					HBox line = new HBox(24, label, minText, minSpin, maxText, maxSpin);
					line.setAlignment(Pos.CENTER_RIGHT);
					HBox.setHgrow(minText, Priority.NEVER);
					HBox.setHgrow(maxText, Priority.NEVER);

					container.getChildren().add(line);
				}
			}

			Button includeButton = new Button("Add LP");

			HBox letterPairIncludes = new HBox(18);
			HBox letterPairHeader = new HBox(24, includeButton, letterPairIncludes);

			includeButton.setOnAction(event -> {
				TextField lpInput = new TextField();

				lpInput.setMaxWidth(50);
				lpInput.setOnKeyReleased(keyEvent -> {
					if (lpInput.getText().length() > 2) {
						lpInput.setText(lpInput.getText(0, 2));
					}
				});

				letterPairIncludes.getChildren().add(lpInput);
				lpInput.requestFocus();
			});

			container.getChildren().add(letterPairHeader);

			masterLetterPairMap.put(type, letterPairIncludes);
			masterBoolMap.put(type, typeBoolMap);
			masterIntMap.put(type, typeIntMap);
		}

		// nasty hack for coupled parities
		Pair<CheckBox, CheckBox> corner = masterBoolMap.getOrDefault(CubicPieceType.CORNER, new HashMap<>()).get("parity");
		Pair<CheckBox, CheckBox> edge = masterBoolMap.getOrDefault(CubicPieceType.EDGE, new HashMap<>()).get("parity");

		if (corner != null && edge != null) {
			corner.getKey().selectedProperty().bindBidirectional(edge.getKey().selectedProperty());
			corner.getValue().selectedProperty().bindBidirectional(edge.getValue().selectedProperty());
		}

		// scramble num
		Label amountPrompt = new Label("Desired # of scrambles");
		Spinner<Integer> amount = new Spinner<>(1, 100, 1);

		container.getChildren().add(new Label(" ")); // gap
		container.setPadding(new Insets(12));

		ScrollPane scrollParent = new ScrollPane(container);
		scrollParent.setVbarPolicy(ScrollPane.ScrollBarPolicy.ALWAYS);

		Button reset = new Button("RESET");
		reset.setOnAction(event -> {
			amount.decrement(amount.getValue()); // clever way to reset

			for (PieceType type : puzzle.getAnalyzingPuzzle().getPieceTypes()) {
				Map<String, Pair<Spinner<Integer>, Spinner<Integer>>> intMap = masterIntMap.get(type);

				for (String intKey : intMap.keySet()) {
					Pair<Spinner<Integer>, Spinner<Integer>> fields = intMap.get(intKey);

					fields.getKey().decrement(fields.getKey().getValue());
					fields.getValue().increment(Integer.MAX_VALUE / 2); // divide by 2 to prevent overflow
				}

				Map<String, Pair<CheckBox, CheckBox>> boolMap = masterBoolMap.get(type);

				for (String boolKey : boolMap.keySet()) {
					Pair<CheckBox, CheckBox> boxes = boolMap.get(boolKey);
					boxes.getKey().setSelected(boolKey.equalsIgnoreCase("bufferallow"));
					boxes.getValue().setSelected(true);
				}

				masterLetterPairMap.getOrDefault(type, new HBox()).getChildren().clear();
			}
		});

		HBox baseline = new HBox(36, reset, amountPrompt, amount);
		baseline.setAlignment(Pos.CENTER_RIGHT);

		parent.getChildren().add(scrollParent);
		parent.getChildren().add(baseline);

		santa.getDialogPane().setContent(parent);
		santa.getDialogPane().setMaxHeight(720);

		santa.setResultConverter(param -> {
			if (param == ButtonType.OK) {
				BldScramble scr = new BldScramble(puzzle.getAnalyzingPuzzle(), puzzle.generateScramblingPuzzle());

				for (PieceType type : puzzle.getAnalyzingPuzzle().getPieceTypes()) {
					Map<String, Pair<Spinner<Integer>, Spinner<Integer>>> intMap = masterIntMap.get(type);
					Map<String, Pair<CheckBox, CheckBox>> boolMap = masterBoolMap.get(type);
					HBox letterPairIncludes = masterLetterPairMap.get(type);

					Pair<Spinner<Integer>, Spinner<Integer>> targets = intMap.get("targets");
					Pair<Spinner<Integer>, Spinner<Integer>> cycles = intMap.get("breakins");
					Pair<CheckBox, CheckBox> parity = boolMap.get("parity");
					Pair<Spinner<Integer>, Spinner<Integer>> preSolved = intMap.get("presolved");
					Pair<Spinner<Integer>, Spinner<Integer>> misOriented = intMap.get("misoriented");
					Pair<CheckBox, CheckBox> bufferSolved = boolMap.get("buffer");

					BooleanCondition buffer = boolFromPair(bufferSolved);

					scr.writeProperties(type,
							intFromPair(targets),
							intFromPair(cycles),
							boolFromPair(parity),
							intFromPair(preSolved),
							intFromPair(misOriented),
							buffer
					);

					if (buffer.getNegative()) {
						scr.setAllowTwistedBuffer(type, boolMap.get("bufferallow").getValue().isSelected());
					}

					List<String> letterPairs = new ArrayList<>();

					for (Node child : letterPairIncludes.getChildren()) {
						if (child instanceof TextField) {
							String pair = ((TextField) child).getText();

							if (pair.length() == 2) {
								letterPairs.add(pair);
							}
						}
					}

					scr.setLetterPairRegex(type, letterPairs);
				}

				return scr;
			} else {
				return null;
			}
		});

		Optional<BldScramble> userScr = santa.showAndWait();
		double height = santa.getHeight();
		BldScramble scr = null;

		while (userScr.isPresent() && !userScr.get().equals(scr)) {
			santa.hide();

			scr = userScr.get();
			int desiredAmount = amount.getValue();
			this.searchAndDisplay(scr, desiredAmount);

			santa.setHeight(height);
			userScr = santa.showAndWait();
		}
	}

	protected void searchAndDisplay(BldScramble scr, int numScrambles) {
		System.out.println(scr.toString()); // FIXME

		this.applyUserConfiguration(scr);

		// TODO scramble score
		Alert quote = new Alert(Alert.AlertType.INFORMATION);
		quote.setTitle("Please wait…");
		quote.setHeaderText("Your scrambles are being searched. Depending on the configuration, this can take a very long time" +
				"\nIn the meantime, be enlightened by some quotes!");

		Label quoteLabel = new Label("Quotes are currently loading\nYou need Internet in order to enjoy this feature");
		quoteLabel.setWrapText(true);
		quoteLabel.setTextAlignment(TextAlignment.JUSTIFY);
		quoteLabel.setFont(Font.font(quoteLabel.getFont().getFamily(), 36));
		quoteLabel.setAlignment(Pos.BASELINE_LEFT);
		Label authorLabel = new Label(";)");
		authorLabel.setFont(Font.font(authorLabel.getFont().getFamily(), FontWeight.BOLD, 18));
		authorLabel.setAlignment(Pos.BASELINE_RIGHT);
		VBox quoteParent = new VBox(12, quoteLabel, authorLabel);

		ProgressBar progress = new ProgressBar();
		progress.setMaxWidth(Double.MAX_VALUE);
		VBox parent = new VBox(12, quoteParent, progress);
		VBox.setVgrow(quoteParent, Priority.ALWAYS);

		quote.getDialogPane().setContent(parent);

		TimerTask showQuotes = new TimerTask() {
			@Override
			public void run() {
				JSON randomQuote = Main.this.getRandomQuote();

				while (randomQuote.get("quote").decodedStringValue().equals(quoteLabel.getText())) {
					randomQuote = Main.this.getRandomQuote();
				}

				String newQuote = randomQuote.get("quote").decodedStringValue();
				String newAuthor = randomQuote.get("author").decodedStringValue();

				FadeTransition quoteFade = new FadeTransition(Duration.millis(960), quoteParent);
				quoteFade.setFromValue(1.0);
				quoteFade.setToValue(0.0);
				quoteFade.setOnFinished(event -> {
					quoteLabel.setText(newQuote);
					authorLabel.setText(newAuthor);
				});

				FadeTransition quoteAppear = new FadeTransition(Duration.millis(960), quoteParent);
				quoteAppear.setFromValue(0.0);
				quoteAppear.setToValue(1.0);

				SequentialTransition changeQuote = new SequentialTransition(quoteFade, quoteAppear);

				Platform.runLater(changeQuote::play);
			}
		};

		Timer quoteTimer = new Timer();
		quoteTimer.scheduleAtFixedRate(showQuotes, 960, 7000);

		Task<Void> scrTask = new Task<Void>() {
			@Override
			protected Void call() throws Exception {
				// GO!! SEARCH!!
				List<Algorithm> scrambles = scr.findScramblesThreadModel(numScrambles, current -> updateProgress(current, numScrambles));

				// Found something!!
				quoteTimer.cancel();

				String joinedScrambles = Main.this.generateJoinedScrambles(scr, scrambles, false);

				TextArea textArea = new TextArea(joinedScrambles);
				textArea.setEditable(false);
				textArea.setWrapText(true);
				textArea.setFont(Font.font(textArea.getFont().getFamily(), 16));

				textArea.setMaxWidth(Double.MAX_VALUE);
				textArea.setMaxHeight(Double.MAX_VALUE);
				GridPane.setVgrow(textArea, Priority.ALWAYS);
				GridPane.setHgrow(textArea, Priority.ALWAYS);

				CheckBox showSolutions = new CheckBox("Show solutions");
				showSolutions.selectedProperty().addListener(
						(observable, oldValue, newValue) -> textArea.setText(Main.this.generateJoinedScrambles(scr, scrambles, newValue))
				);

				GridPane expContent = new GridPane();
				expContent.setVgap(12);
				expContent.setMaxWidth(Double.MAX_VALUE);
				expContent.add(textArea, 0, 0);
				expContent.add(showSolutions, 0, 1);

				Platform.runLater(() -> {
					quote.setAlertType(Alert.AlertType.INFORMATION);
					quote.setTitle("SantaScramble");
					quote.setHeaderText("Found scrambles!");
					quote.getDialogPane().setContent(expContent);
				});

				return null;
			}
		};

		progress.progressProperty().bind(scrTask.progressProperty());

		Thread t = new Thread(scrTask);
		t.setDaemon(true);
		t.start();

		quote.showAndWait();

		quoteTimer.cancel();
		t.interrupt();
	}

	protected JSON getRandomQuote() {
		try {
			Request quoteReq = RestFul.get("http://quote.suushiemaniac.com/api/v1/cubing/random");
			Response quoteResp = quoteReq.send();

			if (quoteResp.getCode() == 200) {
				return JSON.fromString(quoteResp.getBody());
			} else {
				throw new IOException("suushie quote said NO!");
			}
		} catch (IOException e) {
			JSON json = JSON.emptyObject();
			json.set("quote", JSON.fromNative("It seems you have no Internet connection"));
			json.set("author", JSON.fromNative("Please feel free to report this issue if you do"));

			return json;
		}
	}

	protected String generateJoinedScrambles(BldScramble scr, List<Algorithm> scrambles, boolean withSolutions) {
		List<String> scrambleLines = new ArrayList<>();

		BldPuzzle puzzle = scr.generateAnalyzingPuzzle();
		int nthScramble = 0;

		for (Algorithm scramble : scrambles) {
			if (withSolutions) {
				puzzle.parseScramble(scramble);
				scrambleLines.add(scramble.toFormatString() + "\n" + puzzle.getSolutionPairs(true));
			} else {
				scrambleLines.add(++nthScramble + ":\t" + scramble.toFormatString());
			}

		}

		String joinedScrambles = String.join(withSolutions ? "\n\n" : "\n", scrambleLines);

		if (!withSolutions) {
			joinedScrambles += "\n\n\nNot what you were looking for?\nSend your scramble wishes plus the following output to the developer:\n\n" + scr.toString();
		}

		return joinedScrambles;
    }

	protected void applyUserConfiguration(BldScramble scr) {
		JSON config = this.getScrambleConfiguration();

		JSON orientation = config.get("orientation");
		JSON buffers = config.get("buffer");
		JSON lettering = config.get("lettering");

		if (orientation != null) {
			scr.setSolvingOrientation(orientation.get("top").intValue(), orientation.get("front").intValue());
		}

		for (PieceType type : scr.generateAnalyzingPuzzle().getPieceTypes()) {
			if (buffers != null) {
				scr.setBuffer(type, buffers.get(type.mnemonic()).intValue());
			}
			
			if (lettering != null) {
				scr.setLetteringScheme(type, lettering.get(type.mnemonic()).stringValue().split(""));
			}
		}
	}

	protected JSON getScrambleConfiguration() {
    	File configFile = new File(System.getProperty("user.home"), "santaScramble.json");

    	if (configFile.exists()) {
    		return JSON.fromFile(configFile);
		} else {
    		JSON defaultConfig = JSON.emptyObject();

    		JSON defaultOrientation = JSON.emptyObject();
    		JSON defaultBuffers = JSON.emptyObject();
    		JSON defaultLettering = JSON.emptyObject();

    		defaultOrientation.set("top", JSON.fromNative(0));
    		defaultOrientation.set("front", JSON.fromNative(2));

    		defaultBuffers.set("C", JSON.fromNative(0));
    		defaultBuffers.set("E", JSON.fromNative(20));
    		defaultBuffers.set("W", JSON.fromNative(20));
    		defaultBuffers.set("X", JSON.fromNative(0));
    		defaultBuffers.set("T", JSON.fromNative(20));

    		defaultLettering.set("C", JSON.fromNative("ABCDEFGHIJKLMNOPQRSTUVWX"));
    		defaultLettering.set("E", JSON.fromNative("ABCDEFGHIJKLMNOPQRSTUVWX"));
    		defaultLettering.set("W", JSON.fromNative("ABCDEFGHIJKLMNOPQRSTUVWX"));
    		defaultLettering.set("X", JSON.fromNative("ABCDEFGHIJKLMNOPQRSTUVWX"));
    		defaultLettering.set("T", JSON.fromNative("ABCDEFGHIJKLMNOPQRSTUVWX"));

    		defaultConfig.set("orientation", defaultOrientation);
    		defaultConfig.set("buffer", defaultBuffers);
    		defaultConfig.set("lettering", defaultLettering);

    		JSON.toFile(defaultConfig, configFile);
    		return defaultConfig;
		}
	}

    public static void main(String[] args) {
        launch(args);
    }

    protected static IntCondition intFromPair(Pair<Spinner<Integer>, Spinner<Integer>> pair) {
    	int min = pair.getKey().getValue();
    	int max = pair.getValue().getValue();

		if (min > max) {
    		int temp = min;
    		min = max;
    		max = temp;
		}

		return IntCondition.INTERVAL(min, max);
	}

	protected static BooleanCondition boolFromPair(Pair<CheckBox, CheckBox> pair) {
    	boolean truth = pair.getKey().isSelected();
    	boolean matters = !pair.getValue().isSelected();

    	return matters ? truth ? BooleanCondition.YES() : BooleanCondition.NO() : BooleanCondition.MAYBE();
	}
}
