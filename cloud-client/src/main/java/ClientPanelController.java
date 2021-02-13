import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;

import java.io.IOException;
import java.net.URL;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class ClientPanelController implements Initializable {

    @FXML
    TableView<FileInfo> filesTable;

    @FXML
    ComboBox<String> drivesList;

    @FXML
    TextField pathField;

    private ClientController mainController;
    private Path currentPath;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        TableColumn<FileInfo,String> fileTypeColumn = new TableColumn<>("T");
        fileTypeColumn.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getType().getName()));
        fileTypeColumn.setPrefWidth(24);

        TableColumn<FileInfo,FileInfo> fileNameColumn = new TableColumn<>("Name");
        fileNameColumn.setCellValueFactory(param -> new SimpleObjectProperty(param.getValue()));
        fileNameColumn.setPrefWidth(120);
        fileNameColumn.setCellFactory(column -> {
            return new TableCell<FileInfo, FileInfo>() {
                @Override
                protected void updateItem(FileInfo item, boolean empty) {
                    super.updateItem(item, empty);
                    if (item == null || empty) {
                        setText(null);
                        setStyle("");
                    } else {
                        if (item.getType() == FileInfo.FileType.DIR) {
                            super.setStyle("-fx-font-weight: bold");
                        } else {
                            super.setStyle("-fx-font-weight: normal");
                        }

                        super.setText(item.getName());
                    }
                }
            };
        });

        TableColumn<FileInfo,Long> fileSizeColumn = new TableColumn<>("Size");
        fileSizeColumn.setCellValueFactory(param -> new SimpleObjectProperty<>((param.getValue().getSize())));
        fileSizeColumn.setPrefWidth(120);
        fileSizeColumn.setCellFactory(column -> {
            return new TableCell<FileInfo, Long>() {
                @Override
                protected void updateItem(Long item, boolean empty) {
                    super.updateItem(item, empty);
                    if (item == null || empty) {
                        setText(null);
                        setStyle("");
                    } else {
                        if (item == -1L) {
                            setText("DIR");
                        } else {
                            setText(String.format("%,d b",item));
                        }
                    }
                }
            };
        });

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        TableColumn<FileInfo,String> fileModifiedColumn = new TableColumn<>("Modif");
        fileModifiedColumn.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getModified().format(dtf)));
        fileModifiedColumn.setPrefWidth(120);

        drivesList.getItems().clear();
        for (Path p: FileSystems.getDefault().getRootDirectories()) {
            drivesList.getItems().add(p.toString());
        }
        drivesList.getSelectionModel().select(0);

        filesTable.setOnMouseClicked(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                if (event.getClickCount() == 2) {
                    FileInfo item = filesTable.getSelectionModel().getSelectedItem();
                    if (item != null) {
                        Path path = Paths.get(pathField.getText()).resolve(item.getName());
                        if (Files.isDirectory(path))
                            updateList(path);
                    }
                }
            }
        });

        filesTable.getColumns().addAll(fileTypeColumn,fileNameColumn, fileSizeColumn, fileModifiedColumn);
        filesTable.getSortOrder().add(fileTypeColumn);
        updateList(Paths.get("."));
    }

    void updateList() {
        updateList(currentPath);
    }
    void updateList(Path path) {
        try {
            currentPath = path;
            pathField.setText(path.normalize().toAbsolutePath().toString());
            filesTable.getItems().clear();
            filesTable.getItems().addAll(Files.list(path).map(FileInfo::new).collect(Collectors.toList()));
            filesTable.sort();
        } catch (IOException e) {
            Alert alert = new Alert(Alert.AlertType.WARNING,"FS error", ButtonType.OK);
            alert.showAndWait();
        }
    }


    public void pathUp(ActionEvent actionEvent) {
        Path up = Paths.get(pathField.getText()).getParent();
        if (up != null) {
            updateList(up);
        }
    }

    public void selectDrive(ActionEvent actionEvent) {
        ComboBox<String> src = (ComboBox<String>)actionEvent.getSource();
        updateList(Paths.get(src.getSelectionModel().getSelectedItem()));
    }

    public String getSelectedFilename() {
        if (!filesTable.isFocused()) return null;
        return filesTable.getSelectionModel().getSelectedItem().getName();
    }

    public void setCurrentPath(Path currentPath) {
        this.currentPath = currentPath;
    }

    public Path getCurrentPath() {
        return currentPath;
    }

    public void setMainController(ClientController mainController) {
        this.mainController = mainController;
    }
}
