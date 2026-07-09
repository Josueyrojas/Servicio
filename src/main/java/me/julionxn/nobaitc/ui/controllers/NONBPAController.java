package me.julionxn.nobaitc.ui.controllers;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.Modality;
import javafx.stage.Stage;
import me.julionxn.nobaitc.app.MainApplication;
import me.julionxn.nobaitc.doe.design.Design;
import me.julionxn.nobaitc.doe.nonbpa.Fraction;
import me.julionxn.nobaitc.doe.nonbpa.NonbpaGenerator;
import me.julionxn.nobaitc.io.ClipboardHelper;
import me.julionxn.nobaitc.io.ExcelWriter;
import me.julionxn.nobaitc.io.PdfReportWriter;
import me.julionxn.nobaitc.ui.viewmodel.FractionRow;

import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

/**
 * Controlador de la interfaz de generación de fracciones NONBPA.
 *
 * <p>Tras la reestructuración usa el dominio puro: {@link NonbpaGenerator} +
 * {@link Design} en lugar del antiguo {@code NONBPAGeneratorService}, y la tabla
 * trabaja con {@link FractionRow} (view-model) que envuelve a {@link Fraction}.
 * El mapeo dominio→vista ocurre solo en el borde (al recibir resultados).</p>
 */
public class NONBPAController implements Initializable {

    // ==================== FXML Components ====================

    @FXML private GridPane factorsInputContainer;
    @FXML private Button removeFactorButton;
    @FXML private TextField fractionSizeField;
    @FXML private TextField numberOfFractionsField;
    @FXML private RadioButton randomFractionsRadio;
    @FXML private RadioButton customFractionsRadio;
    @FXML private TextField customFractionsField;
    @FXML private VBox tableViewBox, tableFractionsNONBPABox;
    @FXML private ScrollPane inputDataScrollPane;
    @FXML private Button dataInputViewBtn;
    @FXML private Button dataTableViewBtn;

    @FXML private Button sendToAliasBtn;
    @FXML private Button toExcelBtn, toPdfBtn;
    @FXML private Button copyTableBtn;

    @FXML private Label trLabel;
    @FXML private Label factorsCountLabel;
    @FXML private Label lcmLabel;
    @FXML private Label glLabel;
    @FXML private Label sfMinLabel;

    @FXML private TableView<FractionRow> resultsTable;
    @FXML private TableColumn<FractionRow, Integer> fractionNumberColumn;
    @FXML private TableColumn<FractionRow, String> fractionDataColumn;
    @FXML private TableColumn<FractionRow, Double> gbmColumn;
    @FXML private TableColumn<FractionRow, Double> j2Column;
    @FXML private TableColumn<FractionRow, Double> vifsColumn;

    private IntegerProperty sfmin = new SimpleIntegerProperty(0);

    @FXML private TextArea logTextArea;

    @FXML private TableColumn<FractionRow, Long>   startIndexColumn;  // número real
    @FXML private TableColumn<FractionRow, String> gbmVectorColumn;   // GBM por factor
    @FXML private TableColumn<FractionRow, String> aliasColumn;       // estructura de alias

    // ==================== Services & Data ====================

    private final NonbpaGenerator generator;
    private final ObservableList<FractionRow> fractionResults;

    // Constantes
    private static final int MIN_FACTORS = 1;
    private static final int INITIAL_FACTORS = 2;
    private static final int MAX_FACTORS = 15;
    private static final String NUMERIC_REGEX = "\\d*";
    private static final String NUMBER_FORMAT = "%.4f";

    public NONBPAController() {
        this.generator = new NonbpaGenerator();
        this.fractionResults = FXCollections.observableArrayList();
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupUI();
        setupTableColumns();
        setupValidation();
        addInitialFactors();
        updateDesignInfo();
        handleShowDataInput();
        resultsTable.getItems().addListener((ListChangeListener.Change<? extends FractionRow> change) -> {
                    autoSizeColumn(fractionDataColumn);
                    autoSizeColumn(vifsColumn);
        });

        sfmin.addListener((observable, oldValue, newValue) -> {
            fractionSizeField.setText(String.valueOf(newValue));
        });
    }

    // ==================== UI Setup ====================

    private void setupUI() {
        setupRadioButtons();
        setupResultsTable();
        setupLogArea();
        setupCustomFractionsField();
    }

    private void setupRadioButtons() {
        ToggleGroup fractionTypeGroup = new ToggleGroup();
        randomFractionsRadio.setToggleGroup(fractionTypeGroup);
        customFractionsRadio.setToggleGroup(fractionTypeGroup);
        randomFractionsRadio.setSelected(true);
    }

    private void setupResultsTable() {
        resultsTable.setItems(fractionResults);
        resultsTable.setRowFactory(this::createTableRowFactory);
    }

    private TableRow<FractionRow> createTableRowFactory(TableView<FractionRow> tv) {
        TableRow<FractionRow> row = new TableRow<>();

        row.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2 && !row.isEmpty()) {
                FractionRow rowData = row.getItem();
                handleFractionDoubleClick(rowData);
            }
        });

        return row;
    }

    private void handleFractionDoubleClick(FractionRow fractionRow) {
        copyFractionToClipboard(fractionRow);
        openDetailsWindow(fractionRow);
    }

    private void setupLogArea() {
        logTextArea.setEditable(false);
        logTextArea.setWrapText(true);
    }

    private void setupCustomFractionsField() {
        customFractionsRadio.selectedProperty().addListener((obs, oldVal, newVal) ->
                customFractionsField.setDisable(!newVal)
        );
        customFractionsField.setDisable(true);

        customFractionsField.setPromptText("Ej: 1, 2, 5   —   Si desea más de una fracción separe con comas: 1, 2, …");
    }

    private void setupTableColumns() {
        fractionNumberColumn.setCellValueFactory(new PropertyValueFactory<>("fractionNumber"));
        startIndexColumn    .setCellValueFactory(new PropertyValueFactory<>("startIndex"));
        fractionDataColumn  .setCellValueFactory(new PropertyValueFactory<>("fractionData"));
        gbmColumn           .setCellValueFactory(new PropertyValueFactory<>("gbm"));
        gbmVectorColumn     .setCellValueFactory(new PropertyValueFactory<>("gbmVectorData"));
        j2Column            .setCellValueFactory(new PropertyValueFactory<>("j2"));
        vifsColumn          .setCellValueFactory(new PropertyValueFactory<>("vifsData"));
        aliasColumn         .setCellValueFactory(new PropertyValueFactory<>("aliasData"));

        setupNumericColumn(gbmColumn);
        setupNumericColumn(j2Column);
    }

    private void setupNumericColumn(TableColumn<FractionRow, Double> column) {
        column.setCellFactory(tc -> new TableCell<>() {
            @Override
            protected void updateItem(Double value, boolean empty) {
                super.updateItem(value, empty);
                setText(empty || value == null ? null : String.format(NUMBER_FORMAT, value));
            }
        });
    }

    private void setupValidation() {
        addNumericValidation(fractionSizeField, true);
        addNumericValidation(numberOfFractionsField, false);
    }

    private void addNumericValidation(TextField field, boolean updateDesignInfo) {
        field.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.matches(NUMERIC_REGEX)) {
                field.setText(newVal.replaceAll("[^\\d]", ""));
            }
            if (updateDesignInfo) {
                updateDesignInfo();
            }
        });
    }

    // ==================== Factor Management ====================

    private void addInitialFactors() {
        for (int i = 0; i < INITIAL_FACTORS; i++) {
            addFactorInput();
        }
    }

    @FXML
    private void addFactorInput() {
        int currentSize = factorsInputContainer.getChildren().size();

        if (currentSize >= MAX_FACTORS) {
            return;
        }

        if (currentSize > 0) {
            VBox lastBox = (VBox) factorsInputContainer.getChildren().get(currentSize - 1);
            TextField lastLevelField = (TextField) lastBox.getChildren().get(1);
            lastLevelField.setOnMouseClicked(null);
        }

        int columna = currentSize % 5;
        int renglon = currentSize / 5;

        VBox factorBox = createFactorInputBox(currentSize + 1);
        factorsInputContainer.add(factorBox, columna, renglon);

        updateRemoveButtonState();
        updateDesignInfo();
    }

    private VBox createFactorInputBox(int factorNumber) {
        VBox factorBox = new VBox(0);

        Label label = new Label("Factor " + factorNumber + ":");
        label.getStyleClass().add("mathLabel");
        label.setMinWidth(128);

        factorBox.setAlignment(Pos.CENTER_LEFT);
        factorBox.setSpacing(4);

        TextField levelField = createLevelTextField();
        levelField.setMinWidth(64);

        // Autollenado: el factor N se prellena con el N-ésimo número primo
        // (2, 3, 5, 7, 11, …). Los primos son coprimos entre sí, por lo que el
        // diseño resultante cumple TR = LCM y es válido por defecto. El usuario
        // puede sobrescribir el valor si necesita otros niveles.
        levelField.setText(String.valueOf(nthPrime(factorNumber)));

        levelField.setOnMouseClicked(event -> addFactorInput());
        factorBox.getChildren().addAll(label, levelField);

        return factorBox;
    }

    /** Devuelve el n-ésimo número primo (n base 1): 1→2, 2→3, 3→5, 4→7, … */
    private static int nthPrime(int n) {
        int count = 0;
        int candidate = 1;
        while (count < n) {
            candidate++;
            if (isPrime(candidate)) {
                count++;
            }
        }
        return candidate;
    }

    private static boolean isPrime(int x) {
        if (x < 2) {
            return false;
        }
        for (int i = 2; (long) i * i <= x; i++) {
            if (x % i == 0) {
                return false;
            }
        }
        return true;
    }

    private TextField createLevelTextField() {
        TextField levelField = new TextField();
        levelField.setPromptText("Niveles");
        levelField.setPrefWidth(100);

        levelField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.matches(NUMERIC_REGEX)) {
                levelField.setText(newVal.replaceAll("[^\\d]", ""));
            }
            updateDesignInfo();
        });

        return levelField;
    }

    @FXML
    private void removeFactorInput() {
        if (factorsInputContainer.getChildren().size() > MIN_FACTORS) {
            int lastIndex = factorsInputContainer.getChildren().size() - 1;
            factorsInputContainer.getChildren().remove(lastIndex);

            int newSize = factorsInputContainer.getChildren().size();
            if (newSize > 0) {
                VBox newLastBox = (VBox) factorsInputContainer.getChildren().get(newSize - 1);
                TextField newLastLevelField = (TextField) newLastBox.getChildren().get(1);
                newLastLevelField.setOnMouseClicked(event -> addFactorInput());
            }

            updateRemoveButtonState();
            updateDesignInfo();
        }
    }

    private void updateRemoveButtonState() {
        removeFactorButton.setDisable(
                factorsInputContainer.getChildren().size() <= MIN_FACTORS
        );
    }

    // ==================== Design Info ====================

    private void updateDesignInfo() {
        try {
            int[] design = getDesignArray();

            if (design.length == 0) {
                clearDesignInfo();
                return;
            }

            Design designObj = new Design(design);
            Design.DesignParameters params = designObj.parameters();

            displayDesignParameters(params);
            displayValidationStatus(designObj);

        } catch (Exception e) {
            clearDesignInfo();
        }
    }

    public void setSfmin(int value) {
        sfmin.set(value);
    }

    private void displayDesignParameters(Design.DesignParameters params) {
        trLabel.setText("Número de corridas (TR): " + params.tr());
        factorsCountLabel.setText("Factores: " + params.factors());
        lcmLabel.setText("Mínimo común múltiplo (LCM): " + params.lcm());
        glLabel.setText("Grados de libertad (GL): " + params.gl());
        int sfmin = params.sfMin();
        setSfmin(sfmin);
        sfMinLabel.setText("Tamaño minimo de la fraccion (SF min): " + sfmin);
    }

    private void displayValidationStatus(Design design) {
        boolean isValid = design.isValid();
        String status = isValid ?
                "Diseño válido" :
                "Diseño no válido (TR ≠ LCM o > " + MAX_FACTORS + " factores)";

        logTextArea.setText("Estado: " + status);
    }

    private void clearDesignInfo() {
        trLabel.setText("Número de corridas (TR): -");
        factorsCountLabel.setText("Factores: -");
        lcmLabel.setText("Mínimo común múltiplo (LCM): -");
        glLabel.setText("Grados de libertad (GL): -");
        sfMinLabel.setText("Tamaño minimo de la fraccion (SF min): -");
        setSfmin(0);
        logTextArea.clear();
    }

    // ==================== Fraction Generation ====================

    @FXML
    private void generateFractions() {
        try {
            int[] design          = validateAndGetDesign();
            int   fractionSize    = parseIntegerField(fractionSizeField,    "Tamaño de fracción");
            int   numberOfFracts  = parseIntegerField(numberOfFractionsField,"Número de fracciones");

            javafx.scene.control.ProgressIndicator spinner =
                    new javafx.scene.control.ProgressIndicator();
            spinner.setMaxSize(32, 32);
            logTextArea.setVisible(false);
            javafx.scene.layout.VBox logParent =
                    (javafx.scene.layout.VBox) logTextArea.getParent();
            logParent.getChildren().add(0, spinner);

            int[]  designFinal       = design;
            int    fractionSizeFinal = fractionSize;
            int    numberOfFractsFinal = numberOfFracts;

            me.julionxn.nobaitc.util.AppExecutor.execute(
                    () -> generateFractionsBasedOnMode(designFinal, fractionSizeFinal, numberOfFractsFinal),
                    results -> {
                        logParent.getChildren().remove(spinner);
                        logTextArea.setVisible(true);
                        displayResults(results);
                        handleOpenPreview();
                        enableButtons();
                    },
                    error -> {
                        logParent.getChildren().remove(spinner);
                        logTextArea.setVisible(true);
                        if (error instanceof NumberFormatException) {
                            showError("Error de entrada", "Verifique que todos los campos numéricos sean válidos");
                        } else if (error instanceof IllegalArgumentException) {
                            showError("Error de validación", error.getMessage());
                        } else {
                            showError("Error", "Error al generar fracciones: " + error.getMessage());
                            error.printStackTrace();
                        }
                    }
            );

        } catch (NumberFormatException e) {
            showError("Error de entrada", "Verifique que todos los campos numéricos sean válidos");
        } catch (IllegalArgumentException e) {
            showError("Error de validación", e.getMessage());
        } catch (Exception e) {
            showError("Error", "Error al generar fracciones: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private int[] validateAndGetDesign() {
        int[] design = getDesignArray();

        if (design.length == 0) {
            throw new IllegalArgumentException("Ingrese al menos un factor con niveles válidos");
        }

        if (!new Design(design).isValid()) {
            throw new IllegalArgumentException("El diseño no es válido para NONBPA");
        }

        return design;
    }

    private int parseIntegerField(TextField field, String fieldName) {
        String text = field.getText().trim();

        if (text.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " no puede estar vacío");
        }

        return Integer.parseInt(text);
    }

    private List<Fraction> generateFractionsBasedOnMode(
            int[] design, int fractionSize, int numberOfFractions) {

        Design designObj = new Design(design);
        if (randomFractionsRadio.isSelected()) {
            return generator.generateRandom(designObj, fractionSize, numberOfFractions);
        } else {
            List<Integer> customFractions = parseCustomFractions(
                    customFractionsField.getText()
            );
            return generator.generateCustom(designObj, fractionSize, customFractions);
        }
    }

    private void displayResults(List<Fraction> results) {
        fractionResults.clear();
        for (Fraction f : results) {
            fractionResults.add(new FractionRow(f));
        }

        logTextArea.setText(buildResultsSummary(results));
    }

    private String buildResultsSummary(List<Fraction> results) {
        StringBuilder log = new StringBuilder();
        log.append("Generación completada exitosamente!\n\n");
        log.append("Resumen de fracciones generadas:\n");
        log.append("─".repeat(50)).append("\n");

        for (Fraction result : results) {
            log.append(String.format(
                    "Fracción %d - GBM: %.4f, J2: %.4f, Max VIF: %.4f\n",
                    result.getFractionNumber(),
                    result.getGbm(),
                    result.getJ2(),
                    Arrays.stream(result.getVifs()).max().orElse(0.0)
            ));
        }

        log.append("─".repeat(50)).append("\n");
        log.append("Total: ").append(results.size()).append(" fracciones\n");

        return log.toString();
    }

    @FXML
    private void clearResults() {
        fractionResults.clear();
        logTextArea.clear();

        fractionSizeField.clear();
        numberOfFractionsField.clear();
        customFractionsField.clear();

        factorsInputContainer.getChildren().clear();
        addInitialFactors();
        clearDesignInfo();
        disableButtons();
        handleShowDataInput();
    }

    private int[] getDesignArray() {
        return factorsInputContainer.getChildren().stream()
                .map(node -> (VBox) node)
                .map(Vbox -> (TextField) Vbox.getChildren().get(1))
                .map(TextField::getText)
                .filter(text -> !text.isEmpty())
                .mapToInt(Integer::parseInt)
                .filter(level -> level > 0)
                .toArray();
    }

    private List<Integer> parseCustomFractions(String input) {
        if (input == null || input.trim().isEmpty()) {
            throw new IllegalArgumentException("Campo de fracciones personalizadas vacío");
        }

        String cleaned = input.replaceAll("[\\[\\]]", "").trim();
        String[] tokens = cleaned.split("[,\\s]+");

        return Arrays.stream(tokens)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(Integer::parseInt)
                .collect(Collectors.toList());
    }

    public void copyFractionToClipboard(FractionRow fractionRow) {
        double[][] data = fractionRow.getFraction();
        StringBuilder sb = new StringBuilder();

        for (double[] row : data) {
            for (int i = 0; i < row.length; i++) {
                sb.append(row[i]);
                sb.append(i != row.length - 1 ? "\t" : "\n");
            }
        }

        ClipboardHelper.copyToClipboard(sb.toString());
    }

    private void openDetailsWindow(FractionRow data) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    MainApplication.getResourceURL("fxml/fraction-result-details.fxml")
            );
            Parent root = loader.load();
            FractionResultDetailsController controller = loader.getController();
            controller.setData(data.getModel());

            Stage stage = new Stage();
            stage.setTitle("Detalles - Fracción " + data.getFractionNumber());
            stage.setScene(new Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.show();

        } catch (IOException e) {
            showError("Error", "No se pudo abrir la ventana de detalles");
            e.printStackTrace();
        }
    }

    // ==================== Excel Methods ====================
    @FXML
    private void handleExportExcel() {
        List<Fraction> lista = modelsFromTable();
        if (lista.isEmpty()) return;

        Stage mainStage = (Stage) resultsTable.getScene().getWindow();
        ExcelWriter.generateReport(lista, mainStage);
    }

    // ======== PDF Methods ====================
    @FXML
    private void handleExportPdf() {
        List<Fraction> lista = modelsFromTable();
        if (lista.isEmpty()) return;
        Stage mainStage = (Stage) resultsTable.getScene().getWindow();
        PdfReportWriter.generateReport(lista, mainStage);
    }

    @FXML
    private void handleOpenPreview() {
        if (resultsTable.getItems().isEmpty()) return;
        handleShowTableView();
    }

    @FXML
    private void handleOpenPreviewClick(ActionEvent event) {
        openExcelPreview(modelsFromTable());
    }

    public void openExcelPreview(List<Fraction> lista) {
        if (lista == null || lista.isEmpty()) return;
        try {
            FXMLLoader loader = new FXMLLoader(MainApplication.getResourceURL("fxml/excel-preview.fxml"));
            Parent root = loader.load();
            ExcelPreviewController controller = loader.getController();
            controller.initData(lista);

            Stage previewStage = new Stage();
            previewStage.setScene(new javafx.scene.Scene(root));
            previewStage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /** Extrae los modelos de dominio ({@link Fraction}) de las filas de la tabla. */
    private List<Fraction> modelsFromTable() {
        return resultsTable.getItems().stream()
                .map(FractionRow::getModel)
                .collect(Collectors.toList());
    }

    private void updateButtonsStyle(Button activeBtn, Button inactiveBtn) {
        activeBtn.getStyleClass().remove("tabSelectedBtn");
        activeBtn.getStyleClass().remove("tabUnselectedBtn");

        inactiveBtn.getStyleClass().remove("tabSelectedBtn");
        inactiveBtn.getStyleClass().remove("tabUnselectedBtn");

        activeBtn.getStyleClass().add("tabSelectedBtn");
        inactiveBtn.getStyleClass().add("tabUnselectedBtn");
    }

    @FXML
    private void handleCopyToClipboard() {
        FractionRow selected = resultsTable.getSelectionModel().getSelectedItem();

        if (selected == null) {
            showError("Atención", "Por favor, selecciona una fila de la tabla primero.");
            return;
        }

        int fractionNumber = selected.getFractionNumber();
        double[][] matrix = selected.getFraction();
        double gbm = selected.getGbm();
        double j2 = selected.getJ2();
        double[] vifs = selected.getVifs();

        StringBuilder sb = new StringBuilder();
        sb.append("Resumen de Fracción\n");
        sb.append("----------------------------\n");
        sb.append("Número de Fracción: ").append(fractionNumber).append("\n");
        sb.append("GBM: ").append(gbm).append("\n");
        sb.append("J2: ").append(j2).append("\n");
        sb.append("VIFs: ").append(java.util.Arrays.toString(vifs)).append("\n\n");

        sb.append("Datos de la Matriz:\n");

        if (matrix != null) {
            for (int i = 0; i < matrix.length; i++) {
                for (int j = 0; j < matrix[i].length; j++) {
                    sb.append(matrix[i][j]);
                    if (j < matrix[i].length - 1) {
                        sb.append("\t");
                    }
                }
                sb.append("\n");
            }
        } else {
            sb.append("Sin datos de matriz disponibles.");
        }

        ClipboardHelper.copyToClipboard(sb.toString());
    }

    // ==================== Alert Methods ====================

    private void showError(String title, String message) {
        showAlert(Alert.AlertType.ERROR, title, message);
    }

    private void showWarning(String title, String message) {
        showAlert(Alert.AlertType.WARNING, title, message);
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(type.toString());
        alert.setHeaderText(title);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private MainController mainController;

    public void setMainController(MainController mainController) {
        this.mainController = mainController;
    }

    @FXML
    private void sendToAlias(ActionEvent event) {
        String matrixData = getMatrixDataAsString();
        if (mainController != null) {
            mainController.loadAliasModuleWithData(matrixData);
        } else {
            System.err.println("Error: MainController no está enlazado a NONBPAController.");
        }
    }

    private String getMatrixDataAsString() {
        FractionRow selected = resultsTable.getSelectionModel().getSelectedItem();

        if (selected == null) {
            return "";
        }

        double[][] matrix = selected.getFraction();
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < matrix.length; i++) {
            for (int j = 0; j < matrix[i].length; j++) {
                sb.append(matrix[i][j]);
                if (j < matrix[i].length - 1) {
                    sb.append("\t");
                }
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    public void enableButtons() {
        sendToAliasBtn.setDisable(false);
        toExcelBtn.setDisable(false);
        toPdfBtn.setDisable(false);
        copyTableBtn.setDisable(false);
        dataTableViewBtn.setDisable(false);
        dataTableViewBtn.setVisible(true);
    }

    public void disableButtons() {
        sendToAliasBtn.setDisable(true);
        toExcelBtn.setDisable(true);
        toPdfBtn.setDisable(true);
        copyTableBtn.setDisable(true);
        dataTableViewBtn.setDisable(true);
        dataTableViewBtn.setVisible(false);
    }

    @FXML
    private void handleShowDataInput() {
        inputDataScrollPane.setVisible(true);
        inputDataScrollPane.setManaged(true);
        tableViewBox.setVisible(false);
        tableViewBox.setManaged(false);
        tableFractionsNONBPABox.setVisible(false);
        tableFractionsNONBPABox.setManaged(false);
        updateButtonsStyle(dataInputViewBtn, dataTableViewBtn);
    }

    @FXML
    private void handleShowTableView() {
        tableViewBox.setVisible(true);
        tableViewBox.setManaged(true);
        tableFractionsNONBPABox.setVisible(true);
        tableFractionsNONBPABox.setManaged(true);
        inputDataScrollPane.setVisible(false);
        inputDataScrollPane.setManaged(false);

        updateButtonsStyle(dataTableViewBtn, dataInputViewBtn);
    }

    private void autoSizeColumn(TableColumn<FractionRow, ?> column) {
        Text headerText = new Text(column.getText());
        double maxWidth = headerText.getLayoutBounds().getWidth() + 35;

        Text dataTextNode = new Text();

        for (FractionRow item : resultsTable.getItems()) {
            if (column.getCellObservableValue(item) != null) {
                Object cellValue = column.getCellObservableValue(item).getValue();
                if (cellValue != null) {
                    dataTextNode.setText(cellValue.toString());
                    double width = dataTextNode.getLayoutBounds().getWidth() + 20;

                    if (width > maxWidth) {
                        maxWidth = width;
                    }
                }
            }
        }
        column.setPrefWidth(maxWidth);
    }
}
