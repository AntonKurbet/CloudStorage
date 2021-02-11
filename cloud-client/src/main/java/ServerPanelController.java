import io.netty.handler.codec.serialization.ObjectEncoderOutputStream;

import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;

public class ServerPanelController implements Initializable {

    @FXML
    TableView<FileInfo> filesTable;

    @FXML
    TextField pathField;

    private List<FileInfo> files;
    private static final Logger LOG = LoggerFactory.getLogger(ServerPanelController.class);

    private ClientController mainController;
    private String serverPath;

    public void setFiles(List<FileInfo> files) {
        this.files = files;
    }

    public void setServerPath(String serverPath) {
        this.serverPath = serverPath;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        TableColumn<FileInfo, String> fileTypeColumn = new TableColumn<>("T");
        fileTypeColumn.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getType().getName()));
        fileTypeColumn.setPrefWidth(24);

        TableColumn<FileInfo, FileInfo> fileNameColumn = new TableColumn<>("Name");
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

        TableColumn<FileInfo, Long> fileSizeColumn = new TableColumn<>("Size");
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
                            setText(String.format("%,d b", item));
                        }
                    }
                }
            };
        });

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        TableColumn<FileInfo, String> fileModifiedColumn = new TableColumn<>("Modif");
        fileModifiedColumn.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getModified().format(dtf)));
        fileModifiedColumn.setPrefWidth(120);

        filesTable.setOnMouseClicked(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                String filename = filesTable.getSelectionModel().getSelectedItem().getName();
                FileInfo i = files.stream()
                        .filter(f -> filename.equals(f.getName()))
                        .findAny()
                        .orElse(null);

                if (i.getType() == FileInfo.FileType.DIR) {
                    try {
                        mainController.sendCommand(ServerCommand.CD, filename);
                        mainController.sendCommand(ServerCommand.LS);
                    } catch (IOException e) {
                        LOG.error(e.getMessage());
                    }
                }
            }
        });

        filesTable.getColumns().addAll(fileTypeColumn, fileNameColumn, fileSizeColumn, fileModifiedColumn);
        filesTable.getSortOrder().add(fileTypeColumn);
    }

    public void updateList() {
        pathField.setText(serverPath);
        filesTable.getItems().clear();
        filesTable.getItems().addAll(files);
        filesTable.sort();
    }


    public void pathUp(ActionEvent actionEvent) {
        try {
            mainController.sendCommand(ServerCommand.CD, "..");
            mainController.sendCommand(ServerCommand.LS);
            pathField.setText(serverPath);
        } catch (IOException e) {
            LOG.error(e.getMessage());
        }
    }

    public String getSelectedFilename() {
        if (!filesTable.isFocused()) return null;
        return filesTable.getSelectionModel().getSelectedItem().getName();
    }

    public FileInfo getSelectedItem() {
        if (!filesTable.isFocused()) return null;
        return filesTable.getSelectionModel().getSelectedItem();
    }

    public void setMainController(ClientController mainController) {
        this.mainController = mainController;
    }
}
