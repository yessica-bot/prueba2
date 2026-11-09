package cl.duoc.viviendas.controller;

import cl.duoc.viviendas.model.Casa;
import cl.duoc.viviendas.model.Departamento;
import cl.duoc.viviendas.model.Vivienda;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MainController {

    @FXML private ComboBox<String> cbTipoVivienda;
    @FXML private TextField txtCodigo;
    @FXML private TextField txtSuperficie;
    @FXML private TextField txtHabitaciones;

    @FXML private VBox boxDepto;
    @FXML private TextField txtPiso;
    @FXML private CheckBox chkGastoComun;
    @FXML private CheckBox chkEstacionamiento;

    @FXML private VBox boxCasa;
    @FXML private CheckBox chkPatio;

    @FXML private TableView<Vivienda> tblViviendas;
    @FXML private TableColumn<Vivienda, String> colTipo;
    @FXML private TableColumn<Vivienda, String> colCodigo;
    @FXML private TableColumn<Vivienda, Double> colSuperficie;
    @FXML private TableColumn<Vivienda, Integer> colHabitaciones;
    @FXML private TableColumn<Vivienda, String> colCostoArriendo;

    @FXML private Label lblMensaje;

    private final ObservableList<Vivienda> listaViviendas = FXCollections.observableArrayList();
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    private final String ARCHIVO_DEPTOS = "departamentos.json";
    private final String ARCHIVO_CASAS = "casas.json";

    @FXML
    public void initialize() {
        cbTipoVivienda.getItems().addAll("Departamento", "Casa");
        cbTipoVivienda.setValue("Departamento");

        colTipo.setCellValueFactory(new PropertyValueFactory<>("tipo"));
        colCodigo.setCellValueFactory(new PropertyValueFactory<>("codigo"));
        colSuperficie.setCellValueFactory(new PropertyValueFactory<>("superficie"));
        colHabitaciones.setCellValueFactory(new PropertyValueFactory<>("habitaciones"));

        // Formato moneda $XXX.XXX
        NumberFormat formatoMoneda = NumberFormat.getCurrencyInstance(Locale.of("es", "CL"));
        colCostoArriendo.setCellValueFactory(cellData ->
                new SimpleStringProperty(formatoMoneda.format(cellData.getValue().calcularCostoArriendo()))
        );

        tblViviendas.setItems(listaViviendas);

        actualizarHabilitacionCampos();
        cargarDatosDesdeArchivos();
    }

    @FXML
    private void onTipoViviendaChanged() {
        actualizarHabilitacionCampos();
    }

    private void actualizarHabilitacionCampos() {
        boolean esDepto = "Departamento".equals(cbTipoVivienda.getValue());
        boxDepto.setDisable(!esDepto);
        boxCasa.setDisable(esDepto);
    }

    @FXML
    private void onRegistrarClick() {
        String tipo = cbTipoVivienda.getValue();
        String codigo = txtCodigo.getText().trim();

        if (codigo.isEmpty()) {
            mostrarMensaje("Código: no puede estar vacío ni contener solo espacios.", true);
            return;
        }

        double superficie;
        try {
            superficie = Double.parseDouble(txtSuperficie.getText().trim());
            if (superficie < 20 || superficie > 500) {
                mostrarMensaje("Superficie: debe ser un número entre 20 y 500.", true);
                return;
            }
        } catch (NumberFormatException e) {
            mostrarMensaje("Superficie: debe ser un número válido entre 20 y 500.", true);
            return;
        }

        int habitaciones;
        try {
            habitaciones = Integer.parseInt(txtHabitaciones.getText().trim());
            if (habitaciones <= 0) {
                mostrarMensaje("Habitaciones: debe ser un número entero mayor que cero.", true);
                return;
            }
        } catch (NumberFormatException e) {
            mostrarMensaje("Habitaciones: debe ser un número entero mayor que cero.", true);
            return;
        }

        if ("Departamento".equals(tipo)) {
            int piso;
            try {
                piso = Integer.parseInt(txtPiso.getText().trim());
                if (piso <= 0) {
                    mostrarMensaje("Piso: debe ser un número entero mayor que cero.", true);
                    return;
                }
            } catch (NumberFormatException e) {
                mostrarMensaje("Piso: debe ser un número entero mayor que cero.", true);
                return;
            }

            boolean gastoAlDia = chkGastoComun.isSelected();
            boolean estacionamiento = chkEstacionamiento.isSelected();

            Departamento depto = new Departamento(codigo, superficie, habitaciones, piso, gastoAlDia);
            if (estacionamiento) {
                depto.asignarEstacionamiento();
            }

            listaViviendas.add(depto);
            mostrarMensaje(codigo + " (Departamento) registrada correctamente.", false);

        } else {
            boolean tienePatio = chkPatio.isSelected();
            Casa casa = new Casa(codigo, superficie, habitaciones, tienePatio);
            listaViviendas.add(casa);
            mostrarMensaje(codigo + " (Casa) registrada correctamente.", false);
        }

        guardarDatosEnArchivos();
        limpiarFormulario();
    }

    @FXML
    private void onEliminarClick() {
        Vivienda seleccionada = tblViviendas.getSelectionModel().getSelectedItem();
        if (seleccionada == null) {
            mostrarMensaje("Debe seleccionar una vivienda de la tabla para eliminarla.", true);
            return;
        }

        listaViviendas.remove(seleccionada);
        guardarDatosEnArchivos();
        mostrarMensaje(seleccionada.getCodigo() + " eliminada correctamente.", false);
    }

    private void guardarDatosEnArchivos() {
        List<Departamento> deptos = new ArrayList<>();
        List<Casa> casas = new ArrayList<>();

        for (Vivienda v : listaViviendas) {
            if (v instanceof Departamento d) {
                deptos.add(d);
            } else if (v instanceof Casa c) {
                casas.add(c);
            }
        }

        try (FileWriter wDeptos = new FileWriter(ARCHIVO_DEPTOS);
             FileWriter wCasas = new FileWriter(ARCHIVO_CASAS)) {
            gson.toJson(deptos, wDeptos);
            gson.toJson(casas, wCasas);
        } catch (IOException e) {
            mostrarMensaje("Error al guardar los archivos JSON.", true);
        }
    }

    private void cargarDatosDesdeArchivos() {
        listaViviendas.clear();

        File fDeptos = new File(ARCHIVO_DEPTOS);
        if (fDeptos.exists()) {
            try (FileReader rDeptos = new FileReader(fDeptos)) {
                Type listType = new TypeToken<ArrayList<Departamento>>(){}.getType();
                List<Departamento> deptos = gson.fromJson(rDeptos, listType);
                if (deptos != null) listaViviendas.addAll(deptos);
            } catch (IOException e) {
                mostrarMensaje("Error al leer departamentos.json", true);
            }
        }

        File fCasas = new File(ARCHIVO_CASAS);
        if (fCasas.exists()) {
            try (FileReader rCasas = new FileReader(fCasas)) {
                Type listType = new TypeToken<ArrayList<Casa>>(){}.getType();
                List<Casa> casas = gson.fromJson(rCasas, listType);
                if (casas != null) listaViviendas.addAll(casas);
            } catch (IOException e) {
                mostrarMensaje("Error al leer casas.json", true);
            }
        }
    }

    private void limpiarFormulario() {
        txtCodigo.clear();
        txtSuperficie.clear();
        txtHabitaciones.clear();
        txtPiso.clear();
        chkGastoComun.setSelected(false);
        chkEstacionamiento.setSelected(false);
        chkPatio.setSelected(false);
    }

    private void mostrarMensaje(String texto, boolean esError) {
        lblMensaje.setText(texto);
        lblMensaje.setTextFill(esError ? Color.RED : Color.DARKGREEN);
    }
}