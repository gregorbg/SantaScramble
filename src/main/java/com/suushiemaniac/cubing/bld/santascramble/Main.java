package com.suushiemaniac.cubing.bld.santascramble;

import com.suushiemaniac.cubing.alglib.alg.Algorithm;
import com.suushiemaniac.cubing.bld.analyze.BldAnalysis;
import com.suushiemaniac.cubing.bld.filter.BldScramble;
import com.suushiemaniac.cubing.bld.filter.ConditionsBundle;
import com.suushiemaniac.cubing.bld.filter.condition.BooleanCondition;
import com.suushiemaniac.cubing.bld.filter.condition.IntegerCondition;
import com.suushiemaniac.cubing.bld.gsolve.GPuzzle;
import com.suushiemaniac.cubing.bld.gsolve.KPuzzle;
import com.suushiemaniac.cubing.bld.model.PieceType;
import com.suushiemaniac.cubing.bld.model.puzzle.WCAPuzzle;
import com.suushiemaniac.cubing.bld.model.puzzledef.CommandMap;
import com.suushiemaniac.cubing.bld.model.puzzledef.GCommands;
import com.suushiemaniac.lang.json.JSON;
import javafx.animation.FadeTransition;
import javafx.animation.SequentialTransition;
import javafx.application.Application;
import javafx.application.Platform;
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
import org.worldcubeassociation.tnoodle.scrambles.Puzzle;

import java.io.File;
import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class Main extends Application {
    @Override
    public void start(Stage primaryStage) {
        List<WCAPuzzle> choices = new ArrayList<>();
        choices.add(WCAPuzzle.THREE_BLD);
        choices.add(WCAPuzzle.FOUR_BLD);
        choices.add(WCAPuzzle.FIVE_BLD);

        ChoiceDialog<WCAPuzzle> userPick = new ChoiceDialog<>(WCAPuzzle.THREE_BLD, choices);
        userPick.setTitle("SantaScramble");
        userPick.setHeaderText("Which puzzle do you want to generate a scramble for?");
        userPick.setContentText("Please choose:");

        Optional<WCAPuzzle> result = userPick.showAndWait();
        WCAPuzzle choice = null;

        while (result.isPresent() && !result.get().equals(choice)) {
            userPick.hide();

            choice = result.get();
            this.showSantaPanel(choice);

            result = userPick.showAndWait();
        }

        System.exit(0);
    }

    protected void showSantaPanel(WCAPuzzle puzzle) {
        Dialog<BldScramble> santa = new Dialog<>();
        santa.setTitle("SantaScramble - " + puzzle.toString());
        santa.setHeaderText("Please specify what scramble you desire");

        santa.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        VBox parent = new VBox(12);
        VBox container = new VBox(12);

        List<String> boolProperties = new ArrayList<>(Arrays.asList("Parity", "Buffer"));
        List<String> intProperties = new ArrayList<>(Arrays.asList("Targets", "BreakIns", "PreSolved", "MisOriented"));

        Map<String, Function<PieceType, Integer>> maximumBoundMap = new HashMap<>();
        maximumBoundMap.put("Targets", type -> ((type.getPermutationsNoBuffer() / 2) * 3) + (type.getPermutationsNoBuffer() % 2));
        maximumBoundMap.put("BreakIns", type -> type.getPermutationsNoBuffer() / 2);
        maximumBoundMap.put("PreSolved", PieceType::getPermutations);
        maximumBoundMap.put("MisOriented", PieceType::getPermutations);

        Map<PieceType, Map<String, Pair<Spinner<Integer>, Spinner<Integer>>>> masterIntMap = new HashMap<>();
        Map<PieceType, Map<String, Pair<CheckBox, CheckBox>>> masterBoolMap = new HashMap<>();
        Map<PieceType, HBox> masterLetterPairMap = new HashMap<>();
        Map<PieceType, CheckBox> masterScrambleMap = new HashMap<>();

        for (PieceType type : puzzle.getKPuzzle().getPieceTypes()) {
            if (type.getName().equals("CENTER")) {
                continue;
            }

            VBox pieceTypeContainer = new VBox(12);

            Map<String, Pair<Spinner<Integer>, Spinner<Integer>>> typeIntMap = new HashMap<>();
            Map<String, Pair<CheckBox, CheckBox>> typeBoolMap = new HashMap<>();

            HBox heading = new HBox(8);
            Label headingLabel = new Label(type.getHumanName());
            headingLabel.setFont(Font.font(headingLabel.getFont().getFamily(), FontWeight.BOLD, headingLabel.getFont().getSize()));
            heading.getChildren().add(headingLabel);

			/*if (puzzle.getKTag().equals("333")) {
				CheckBox scrambleBox = new CheckBox("Scramble this part?");
				pieceTypeContainer.disableProperty().bind(scrambleBox.selectedProperty().not());

				masterScrambleMap.put(type, scrambleBox);

				scrambleBox.setSelected(true);
				heading.getChildren().add(scrambleBox);
			} else*/
            {
                CheckBox mockBox = new CheckBox();
                mockBox.setSelected(true);

                masterScrambleMap.put(type, mockBox);
            }

            container.getChildren().add(heading);

			/*if (puzzle.getAnalyzingPuzzle() instanceof BldCube
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

				pieceTypeContainer.getChildren().add(methodBox);
			}*/

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

                pieceTypeContainer.getChildren().add(line);
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

                if (!intProperty.equals("MisOriented") || type.getOrientations() > 1) {
                    Label label = new Label("# " + intProperty);

                    HBox line = new HBox(24, label, minText, minSpin, maxText, maxSpin);
                    line.setAlignment(Pos.CENTER_RIGHT);
                    HBox.setHgrow(minText, Priority.NEVER);
                    HBox.setHgrow(maxText, Priority.NEVER);

                    pieceTypeContainer.getChildren().add(line);
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

            pieceTypeContainer.getChildren().add(letterPairHeader);

            masterLetterPairMap.put(type, letterPairIncludes);
            masterBoolMap.put(type, typeBoolMap);
            masterIntMap.put(type, typeIntMap);

            container.getChildren().add(pieceTypeContainer);
        }

        Set<PieceType> pieceTypesForParity = puzzle.getKPuzzle().getPieceTypes();

        // nasty hack for coupled parities - can we skip this with KPuzzle configuration?
        PieceType cornerPieceType = pieceTypesForParity.stream().filter(pieceType -> pieceType.getName().equals("CORNER")).findFirst().orElse(null);
        PieceType edgePieceType = pieceTypesForParity.stream().filter(pieceType -> pieceType.getName().equals("EDGE")).findFirst().orElse(null);

        if (cornerPieceType != null && edgePieceType != null) {
            Pair<CheckBox, CheckBox> corner = masterBoolMap.getOrDefault(cornerPieceType, new HashMap<>()).get("parity");
            Pair<CheckBox, CheckBox> edge = masterBoolMap.getOrDefault(edgePieceType, new HashMap<>()).get("parity");

            if (corner != null && edge != null) {
                corner.getKey().selectedProperty().bindBidirectional(edge.getKey().selectedProperty());
                corner.getValue().selectedProperty().bindBidirectional(edge.getValue().selectedProperty());
            }
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

            for (PieceType type : puzzle.getKPuzzle().getPieceTypes()) {
                if (type.getName().equals("CENTER")) {
                    continue;
                }

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
                GPuzzle gPuzzle = loadGPuzzle(puzzle.getKPuzzle());

                Supplier<Puzzle> scrambleSupplier = () -> {
                    Puzzle tNoodle = puzzle.getTPuzzleSupply().invoke();

					/*if (tNoodle instanceof ThreeByThreeCubePuzzle) {
						boolean scrambleCorners = masterScrambleMap.get(CubicPieceType.CORNER).isSelected();
						boolean scrambleEdges = masterScrambleMap.get(CubicPieceType.EDGE).isSelected();
						boolean isRandom = scrambleCorners && scrambleEdges;

						if (!isRandom) {
							ThreeByThreeCubePuzzle castTNoodle = (ThreeByThreeCubePuzzle) tNoodle;

							byte[] cp = new byte[0];
							byte[] co = new byte[0];

							if (scrambleCorners) {
								cp = null;
								co = null;
							}

							byte[] ep = new byte[0];
							byte[] eo = new byte[0];

							if (scrambleEdges) {
								ep = null;
								eo = null;
							}

							castTNoodle.setCustomConfig(new byte[][]{
									cp,
									co,
									ep,
									eo
							});

							return castTNoodle;
						}
					}*/

                    return tNoodle;
                };

                List<ConditionsBundle> conditions = new ArrayList<>();

                for (PieceType type : puzzle.getKPuzzle().getPieceTypes()) {
                    if (type.getName().equals("CENTER")) {
                        continue;
                    }

                    if (!masterScrambleMap.get(type).isSelected()) {
                        continue;
                    }

                    Map<String, Pair<Spinner<Integer>, Spinner<Integer>>> intMap = masterIntMap.get(type);
                    Map<String, Pair<CheckBox, CheckBox>> boolMap = masterBoolMap.get(type);
                    HBox letterPairIncludes = masterLetterPairMap.get(type);

                    Pair<Spinner<Integer>, Spinner<Integer>> targets = intMap.get("targets");
                    Pair<Spinner<Integer>, Spinner<Integer>> cycles = intMap.get("breakins");
                    Pair<CheckBox, CheckBox> parity = boolMap.get("parity");
                    Pair<Spinner<Integer>, Spinner<Integer>> preSolved = intMap.get("presolved");
                    Pair<Spinner<Integer>, Spinner<Integer>> misOriented = intMap.get("misoriented");
                    Pair<CheckBox, CheckBox> bufferSolved = boolMap.get("buffer");

                    ConditionsBundle bundle = new ConditionsBundle(type,
                            intFromPair(targets),
                            intFromPair(cycles),
                            intFromPair(preSolved),
                            intFromPair(misOriented),
                            boolFromPair(parity),
                            boolFromPair(bufferSolved),
                            boolMap.get("bufferallow").getValue().isSelected()
                    );

                    List<String> letterPairs = new ArrayList<>();

                    for (Node child : letterPairIncludes.getChildren()) {
                        if (child instanceof TextField) {
                            String pair = ((TextField) child).getText();

                            if (pair.length() == 2) {
                                letterPairs.add(pair);
                            }
                        }
                    }

                    if (!letterPairs.isEmpty()) {
                        String[] letterScheme = gPuzzle.getLetterSchemes().get(type);
                        bundle.setLetterPairRegex(letterScheme, letterPairs, true, true);
                    }

                    conditions.add(bundle);
                }

                return new BldScramble(gPuzzle, conditions, () -> {
                    Puzzle tNoodle = scrambleSupplier.get();
                    String rawScramble = tNoodle.generateScramble();

                    return puzzle.getAntlrReader().parse(rawScramble);
                });
            } else {
                return null;
            }
        });

        Optional<BldScramble> userScr = santa.showAndWait();
        double height = santa.getHeight();
        BldScramble scr = null;

        while (userScr.isPresent() && !userScr.get().equals(scr)) {
            System.out.println("Hello");
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
                String newQuote = "Please hold the line…";
                String newAuthor = "The programmer";

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

        Task<Void> scrTask = new Task<>() {
            @Override
            protected Void call() {
                // GO!! SEARCH!!
                List<Algorithm> scrambles = scr.findScramblesThreadModel(numScrambles);

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

        //progress.progressProperty().bind(scrTask.progressProperty());
        progress.setProgress(ProgressIndicator.INDETERMINATE_PROGRESS); // FIXME

        Thread t = new Thread(scrTask);
        t.setDaemon(true);
        t.start();

        quote.showAndWait();

        quoteTimer.cancel();
        t.interrupt();
    }

    protected String generateJoinedScrambles(BldScramble scr, List<Algorithm> scrambles, boolean withSolutions) {
        List<String> scrambleLines = new ArrayList<>();

        GPuzzle puzzle = scr.getAnalyzer();
        int nthScramble = 0;

        for (Algorithm scramble : scrambles) {
            if (withSolutions) {
                BldAnalysis analysis = puzzle.getAnalysis(scramble);
                scrambleLines.add(scramble.toString() + "\n" + analysis.getSolutionPairs(true));
            } else {
                scrambleLines.add(++nthScramble + ":\t" + scramble.toString());
            }
        }

        String joinedScrambles = String.join(withSolutions ? "\n\n" : "\n", scrambleLines);

        if (!withSolutions) {
            joinedScrambles += "\n\n\nNot what you were looking for?\nSend your scramble wishes plus the following output to the developer:\n\n" + scr.toString();
        }

        return joinedScrambles;
    }

    protected GPuzzle loadGPuzzle(KPuzzle basePuzzle) {
        JSON config = this.getScrambleConfiguration();

        JSON orientation = config.get("orientation");
        JSON buffers = config.get("buffer");
        JSON lettering = config.get("lettering");

        Map<String, List<List<String>>> commands = new HashMap<>();
        commands.put("Puzzle", Collections.singletonList(Collections.singletonList(basePuzzle.getName())));

        if (orientation != null) {
            List<String> orientationCmd = new ArrayList<>();
            orientationCmd.add("Fixed");

            int top = orientation.get("top").intValue() + 1;
            int front = orientation.get("front").intValue() + 1;

            orientationCmd.add("CENTER\n" + top + " ? " + front + " ? ? ?");
            commands.put("Orientation", Collections.singletonList(orientationCmd));
        }

        GPuzzle letteringHelper = GPuzzle.Companion.preInstalledConfig(basePuzzle.getName(), "__SPEFFZ", basePuzzle.getReader());

        List<List<String>> bufferCommands = new ArrayList<>();

        if (buffers != null) {
            for (PieceType type : basePuzzle.getPieceTypes()) {
                if (!buffers.nativeKeySet().contains(type.getName())) {
                    continue;
                }

                String[] speffzSchemeArr = letteringHelper.getLetterSchemes().get(type);
                String speffzScheme = String.join("", speffzSchemeArr);

                char bufferTargetLetter = (char) ('A' + buffers.get(type.getName()).intValue());
                int bufferTarget = speffzScheme.indexOf(bufferTargetLetter) + type.getOrientations();

                int bufferPerm = bufferTarget / type.getOrientations();
                int bufferOrient = bufferTarget % type.getOrientations();

                List<String> bufferCommand = Arrays.asList(type.getName(), String.valueOf(bufferPerm), String.valueOf(bufferOrient));
                bufferCommands.add(bufferCommand);
            }

            commands.put("Buffer", bufferCommands);
        }

        List<String> letteringLines = new ArrayList<>();

        if (lettering != null) {
            for (PieceType type : basePuzzle.getPieceTypes()) {
                if (!lettering.nativeKeySet().contains(type.getName())) {
                    continue;
                }

                letteringLines.add(type.getName());

                String[] speffzSchemeArr = letteringHelper.getLetterSchemes().get(type);
                List<Integer> lookupIndices = Arrays.stream(speffzSchemeArr).map(s -> s.charAt(0) - 'A')
                        .collect(Collectors.toList());

                String[] letteringScheme = lettering.get(type.getName()).stringValue().split("");
                String letteringSchemeDefinition = lookupIndices.stream().map(i -> letteringScheme[i]).collect(Collectors.joining(" "));

                letteringLines.add(letteringSchemeDefinition);
            }

            List<String> letteringArg = Collections.singletonList(String.join("\n", letteringLines));
            commands.put("Lettering", Collections.singletonList(letteringArg));
        }

        // defaults.
        commands.put("MisOrient", Collections.singletonList(Collections.singletonList("Compound")));
        commands.put("Execution", Collections.singletonList(new ArrayList<>(buffers.nativeKeySet())));

        CommandMap commandMap = new CommandMap(commands);
        GCommands parsedCommands = GCommands.Companion.parse(commandMap, basePuzzle.getPieceTypes(), basePuzzle.getReader());

        return new GPuzzle(parsedCommands, basePuzzle);
    }

    protected JSON getScrambleConfiguration() {
        File configFile = new File(System.getProperty("user.home"), "santaScramble.json");

        if (configFile.exists()) {
            return JSON.Companion.fromFile(configFile);
        } else {
            JSON defaultConfig = JSON.Companion.emptyObject();

            JSON defaultOrientation = JSON.Companion.emptyObject();
            JSON defaultBuffers = JSON.Companion.emptyObject();
            JSON defaultLettering = JSON.Companion.emptyObject();

            defaultOrientation.set("top", JSON.Companion.fromNative(0));
            defaultOrientation.set("front", JSON.Companion.fromNative(2));

            defaultBuffers.set("CORNER", JSON.Companion.fromNative(0));
            defaultBuffers.set("EDGE", JSON.Companion.fromNative(20));
            defaultBuffers.set("WING", JSON.Companion.fromNative(20));
            defaultBuffers.set("X_CENTER", JSON.Companion.fromNative(0));
            defaultBuffers.set("T_CENTER", JSON.Companion.fromNative(20));

            defaultLettering.set("CORNER", JSON.Companion.fromNative("ABCDEFGHIJKLMNOPQRSTUVWX"));
            defaultLettering.set("EDGE", JSON.Companion.fromNative("ABCDEFGHIJKLMNOPQRSTUVWX"));
            defaultLettering.set("WING", JSON.Companion.fromNative("ABCDEFGHIJKLMNOPQRSTUVWX"));
            defaultLettering.set("X_CENTER", JSON.Companion.fromNative("ABCDEFGHIJKLMNOPQRSTUVWX"));
            defaultLettering.set("T_CENTER", JSON.Companion.fromNative("ABCDEFGHIJKLMNOPQRSTUVWX"));

            defaultConfig.set("orientation", defaultOrientation);
            defaultConfig.set("buffer", defaultBuffers);
            defaultConfig.set("lettering", defaultLettering);

            JSON.Companion.toFile(defaultConfig, configFile);
            return defaultConfig;
        }
    }

    public static void main(String[] args) {
        launch(args);
    }

    protected static IntegerCondition intFromPair(Pair<Spinner<Integer>, Spinner<Integer>> pair) {
        int min = pair.getKey().getValue();
        int max = pair.getValue().getValue();

        if (min > max) {
            int temp = min;
            min = max;
            max = temp;
        }

        return IntegerCondition.Companion.INTERVAL(min, max);
    }

    protected static BooleanCondition boolFromPair(Pair<CheckBox, CheckBox> pair) {
        boolean truth = pair.getKey().isSelected();
        boolean matters = !pair.getValue().isSelected();

        return matters ? truth ? BooleanCondition.Companion.YES() : BooleanCondition.Companion.NO() : BooleanCondition.Companion.MAYBE();
    }
}
