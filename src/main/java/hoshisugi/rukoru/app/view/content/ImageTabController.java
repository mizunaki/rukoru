package hoshisugi.rukoru.app.view.content;

import java.net.URL;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

import com.google.inject.Inject;

import hoshisugi.rukoru.app.enums.MachineImageState;
import hoshisugi.rukoru.app.models.AuthSetting;
import hoshisugi.rukoru.app.models.MachineImage;
import hoshisugi.rukoru.app.services.ec2.EC2Service;
import hoshisugi.rukoru.app.view.popup.CreateInstanceController;
import hoshisugi.rukoru.flamework.controls.BaseController;
import hoshisugi.rukoru.flamework.controls.GraphicTableCell;
import hoshisugi.rukoru.flamework.controls.TextFillTableCell;
import hoshisugi.rukoru.flamework.util.AssetUtil;
import hoshisugi.rukoru.flamework.util.ConcurrentUtil;
import hoshisugi.rukoru.flamework.util.DialogUtil;
import hoshisugi.rukoru.flamework.util.FXUtil;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener.Change;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.image.ImageView;
import javafx.scene.paint.Color;

public class ImageTabController extends BaseController {

	@FXML
	private TableView<MachineImage> tableView;

	@FXML
	private TableColumn<MachineImage, String> nameColumn;

	@FXML
	private TableColumn<MachineImage, String> stateColumn;

	@FXML
	private TableColumn<MachineImage, String> creationDateColumn;

	@FXML
	private TableColumn<MachineImage, MachineImage> createColumn;

	@FXML
	private TableColumn<MachineImage, MachineImage> deregisterColumn;

	@FXML
	private Button refreshButton;

	@Inject
	private EC2Service ec2Service;

	private final ObservableList<MachineImage> items = FXCollections.observableArrayList();

	public ObservableList<MachineImage> getItems() {
		return items;
	}

	@Override
	public void initialize(final URL url, final ResourceBundle resource) {
		refreshButton.setGraphic(new ImageView(AssetUtil.getImage("refresh_24x24.png")));
		stateColumn.setCellFactory(TextFillTableCell.forTableCellFactory(this::provideColor));
		createColumn.setCellValueFactory(GraphicTableCell.forTableCellValueFactory());
		createColumn.setCellFactory(GraphicTableCell.forTableCellFactory(this::createCreateButton));
		deregisterColumn.setCellValueFactory(GraphicTableCell.forTableCellValueFactory());
		deregisterColumn.setCellFactory(GraphicTableCell.forTableCellFactory(this::createDeregisterButton));
		tableView.setItems(items);
		items.addListener(this::onItemsChanged);
		ConcurrentUtil.run(this::loadImages);
	}

	@FXML
	private void onRefreshButtonClick(final ActionEvent event) {
		ConcurrentUtil.run(this::loadImages);
	}

	private void loadImages() {
		try {
			Platform.runLater(() -> refreshButton.setDisable(true));
			items.clear();
			if (AuthSetting.hasSetting()) {
				final List<MachineImage> images = ec2Service.listImages();
				items.addAll(images);
			}
		} finally {
			Platform.runLater(() -> refreshButton.setDisable(false));
		}
	}

	private Button createCreateButton(final MachineImage entity) {
		final Button button = new Button("作成");
		button.setGraphic(new ImageView(AssetUtil.getImage("add_16x16.png")));
		button.setOnAction(this::onCreateButtonClick);
		button.setUserData(entity);
		return button;
	}

	private Button createDeregisterButton(final MachineImage entity) {
		final Button button = new Button("登録解除");
		button.setGraphic(new ImageView(AssetUtil.getImage("delete_16x16.png")));
		button.setOnAction(this::onDeregisterButtonClick);
		button.setUserData(entity);
		return button;
	}

	private void onCreateButtonClick(final ActionEvent event) {
		final Button button = (Button) event.getSource();
		final MachineImage entity = (MachineImage) button.getUserData();
		final CreateInstanceController controller = FXUtil.popup(CreateInstanceController.class,
				FXUtil.getStage(event));
		controller.targetProperty().set(entity);
	}

	private void onDeregisterButtonClick(final ActionEvent event) {
		final Button button = (Button) event.getSource();
		final MachineImage image = (MachineImage) button.getUserData();
		final Optional<ButtonType> response = DialogUtil.showConfirmDialog("登録解除",
				String.format("[%s] の登録を解除します。よろしいですか？", image.getName()));
		if (!response.map(type -> type == ButtonType.OK).orElse(false)) {
			return;
		}

		ConcurrentUtil.run(() -> {
			if (AuthSetting.hasSetting()) {
				ec2Service.deregisterMachineImage(image);
				items.remove(image);
			}
		});
	}

	private void onItemsChanged(final Change<? extends MachineImage> change) {
		if (items.stream().noneMatch(EC2Service::needMonitoring)) {
			return;
		}
		ConcurrentUtil.run(() -> {
			if (AuthSetting.hasSetting()) {
				final List<MachineImage> instances = items.stream().filter(EC2Service::needMonitoring)
						.collect(Collectors.toList());
				ec2Service.monitorImages(instances);
			}
		});
	}

	private Color provideColor(final String state) {
		final MachineImageState s = MachineImageState.of(state);
		final Color color;
		switch (s) {
		case Available:
			color = Color.GREEN;
			break;
		case Pending:
			color = Color.GOLDENROD;
			break;
		default:
			color = null;
		}
		return color;
	}
}
