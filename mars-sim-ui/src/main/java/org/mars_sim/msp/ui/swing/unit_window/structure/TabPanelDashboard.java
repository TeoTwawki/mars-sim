/**
 * Mars Simulation Project
 * TabPanelDashboard.java
 * @version 3.1.0 2017-03-08
 * @author Manny Kung
 */
package org.mars_sim.msp.ui.swing.unit_window.structure;

import java.awt.Dimension;
import java.util.ArrayList;
import java.util.List;

import org.mars_sim.msp.core.Msg;
import org.mars_sim.msp.core.structure.ObjectiveType;
import org.mars_sim.msp.core.structure.Settlement;
import org.mars_sim.msp.ui.javafx.MainScene;
import org.mars_sim.msp.ui.swing.MainDesktopPane;
import org.mars_sim.msp.ui.swing.unit_window.TabPanel;

import com.jfoenix.controls.JFXButton;

import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.geometry.Pos;
import javafx.geometry.Insets;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.control.Label;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;
import javafx.scene.effect.Reflection;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.control.ToggleButton;

/**
 * Tab panel displaying general info regarding the settlement <br>
 */
@SuppressWarnings("restriction")
public class TabPanelDashboard extends TabPanel {

	/** default serial id. */
	//private static final long serialVersionUID = 1L;
	private static final int width = 400;
	private static final int height = 500;
	
	private int themeCache = -1;
	//private double progress;
	//private long lastTimerCall;
	//private boolean toggle, running = false;

	private ObjectiveType objectiveTypeCache;
	private static ObjectiveType[] objectives;
	
	// Data members
	private JFXPanel jfxpanel;
	private Scene scene;
	private StackPane stack, commitPane;
	private Label choiceLabel, choiceHeader, headerLabel;

	// private ToggleGroup group;
	private ToggleButton toggleBtn;
	//private SubmitButton commitButton;
	private JFXButton commitButton;
	private VBox mainBox, headerBox, optionBox;
	private HBox row0, row1, choiceBox;
	private AnimationTimer timer;

	private List<ToggleButton> buttons = new ArrayList<>();

	private Settlement settlement;

	/**
	 * Constructor.
	 *
	 * @param settlement
	 *            {@link Settlement} the settlement this tab panel is for.
	 * @param desktop
	 *            {@link MainDesktopPane} the main desktop panel.
	 */
	@SuppressWarnings("restriction")
	public TabPanelDashboard(Settlement settlement, MainDesktopPane desktop) {
		// Use the TabPanel constructor
		super(Msg.getString("TabPanelDashboard.title"), //$NON-NLS-1$
				null, Msg.getString("TabPanelDashboard.title.tooltip"), //$NON-NLS-1$
				settlement, desktop);

		this.settlement = settlement;
		objectives = settlement.getObjectives();
		createButtonPane();

		jfxpanel = new JFXPanel();

		Platform.runLater(new Runnable() {
			@Override
			public void run() {
				stack = new StackPane();
				scene = new Scene(stack, width, height);
				scene.setFill(Color.TRANSPARENT);// .BLACK);
				jfxpanel.setScene(scene);

				Label title = new Label(Msg.getString("TabPanelDashboard.title"));
				Reflection reflection = new Reflection();
				title.setEffect(reflection);
				reflection.setTopOffset(0.0);
				title.setPadding(new Insets(5, 5, 0, 5));
				// title.setFont(new Font("Arial", 20));
				title.setFont(Font.font("Cambria", FontWeight.BOLD, 16));

				VBox toggleVBox = new VBox();
				//toggleVBox.setStyle("-fx-border-style: 2px; " + "-fx-background-color: #c1bf9d;"
				//		+ "-fx-border-color: #c1bf9d;" + "-fx-background-radius: 2px;");
				toggleVBox.getChildren().addAll(mainBox);
				toggleVBox.setAlignment(Pos.TOP_CENTER);
				toggleVBox.setPadding(new Insets(5, 5, 5, 5));

				VBox topVBox = new VBox();
				//topVBox.setStyle("-fx-border-style: 2px; " + "-fx-background-color: #c1bf9d;"
				//		+ "-fx-border-color: #c1bf9d;" + "-fx-background-radius: 2px;");
				topVBox.setAlignment(Pos.TOP_CENTER);
				topVBox.getChildren().addAll(title, new Label(), toggleVBox);

				stack.getChildren().add(topVBox);
			}
		});

		centerContentPanel.add(jfxpanel);
		this.setSize(new Dimension(width, height));
		this.setVisible(true);

		update();
	}

	public String addSpace(String s) {
		s = s.replace(" ", System.lineSeparator());
		return s;
	}

	public void createButtonPane() {
		ToggleGroup group = new ToggleGroup();
		mainBox = new VBox();

		headerBox = new VBox();
		String header = "  Select a new objective below : ";
		headerLabel = new Label(header);
		headerLabel.setMaxWidth(Double.MAX_VALUE);
		headerBox.getChildren().add(headerLabel);
		headerBox.setMaxWidth(Double.MAX_VALUE);
		
		int size = objectives.length;
		for (int i = 0; i < size; i++) {

			//int index = i;
			ObjectiveType ot = objectives[i];
			String s = ot.toString();
			String ss = null;
			if (i == 0)
				ss = "/icons/settlement_goals/cropfarm.png";
			else if (i == 1)
				ss = "/icons/settlement_goals/manufacture.png";
			else if (i == 2)
				ss = "/icons/settlement_goals/research.png";
			else if (i == 3)
				ss = "/icons/settlement_goals/transport.png";
			else if (i == 4)
				ss = "/icons/settlement_goals/trade.png";
			else if (i == 5)
				ss = "/icons/settlement_goals/trip_128.png";//free_market_128.png";

			toggleBtn = new ToggleButton();
			toggleBtn.setGraphic(new ImageView(new Image(this.getClass().getResourceAsStream(ss))));
			buttons.add(toggleBtn);
			
			String sss = addSpace(s);

			toggleBtn.setPadding(new Insets(5, 5, 5, 5));
			toggleBtn.setTooltip(new Tooltip(sss));
			// btn.setStyle("-fx-alignment: LEFT;");
			toggleBtn.setAlignment(Pos.BASELINE_CENTER);
			toggleBtn.setMaxHeight(90);
			toggleBtn.setMaxWidth(90);
			toggleBtn.setToggleGroup(group);
			toggleBtn.setSelected(true);
			
			group.selectedToggleProperty().addListener(new ChangeListener<Toggle>() {
			    public void changed(ObservableValue<? extends Toggle> ov, Toggle old_toggle, Toggle new_toggle) {

			    	if (new_toggle != null && toggleBtn.isSelected()) {
						// set the objective at the beginning of the sim to the default value
						if (settlement.getObjective() == ot) {
							objectiveTypeCache = ot;
						}

			        }
			     }
			});


		}

		row0 = new HBox();
		row0.getChildren().addAll(buttons.get(0), buttons.get(1), buttons.get(2));
		
		row1 = new HBox();
		row1.getChildren().addAll(buttons.get(3), buttons.get(4), buttons.get(5));

		createCommitButton();
		
		commitPane = new StackPane(commitButton);
		commitPane.setPrefSize(100, 50);
		
		optionBox = new VBox();
		optionBox.getChildren().addAll(row0, row1, commitPane);
		
		choiceHeader = new Label("    Current Choice : ");
		choiceHeader.setMinWidth(140);
		choiceLabel = new Label(settlement.getObjective().toString());
		choiceLabel.setAlignment(Pos.CENTER);
		choiceLabel.setMinWidth(280);
		
		choiceBox = new HBox();
		choiceBox.setMaxWidth(Double.MAX_VALUE);
		choiceBox.setAlignment(Pos.CENTER);
		choiceBox.getChildren().addAll(choiceHeader, choiceLabel);
		
		mainBox.getChildren().addAll(headerBox, optionBox, choiceBox);

	}


	public void createCommitButton() {
		commitButton = new JFXButton("Commit Change");//SubmitButton();
		commitButton.setOnAction(e -> { //setOnMousePressed(e -> {
			
            commitObjective();
/*            
        	if (!running) {
        		timer.start();
                running = true;
                commitButton.setDisable(true);
        	}

        	e.consume();
*/        	
		});

		commitButton.setId("commit-button");
		commitButton.setMaxWidth(150);
		commitButton.setAlignment(Pos.CENTER);
		
		//commitButton.statusProperty().addListener(o -> System.out.println(commitButton.getStatus()));
/*
		progress = 0;
		lastTimerCall = System.nanoTime();
		timer = new AnimationTimer() {
			@Override
			public void handle(long now) {
                if (now > lastTimerCall + 2_000l) {
					progress += 0.005;
					commitButton.setProgress(progress);
					lastTimerCall = now;

                    if (toggle) {
                        if (progress > 0.75) {
                            progress = 0;
                            commitButton.setFailed();
                            timer.stop();
                            running = false;
                            toggle ^= true;
                            commitButton.setDisable(false);
                            // reset back to the old objective
                            resetButton();
                        }

                    } else {
                        if (progress > 1) {
                            progress = 0;
                            commitButton.setSuccess();
                            timer.stop();
                            running = false;
                            toggle ^= true;
                            commitButton.setDisable(false);
                            // set to the new objective
                            commitObjective();
                        }
                    }
				}
			}
		};
*/		
	}

	public void resetButton() {

		for (int i=0; i < objectives.length; i++) {
			if (objectives[i] == settlement.getObjective()) {
				buttons.get(i).setSelected(true);
				break;
			}
		}
	}

	public void commitObjective() {

		if (buttons.size() == 6) {
			// after all the toggleBtn has been setup
			int index = -1;
			for (int j = 0; j < 6; j++) {
				if (buttons.get(j).isSelected()) {
					index = j;
				}
			}

			ObjectiveType type = null;

			if (index == 0)
				type = ObjectiveType.CROP_FARM;
			else if (index == 1)
				type = ObjectiveType.MANUFACTURING;
			else if (index == 2)
				type = ObjectiveType.RESEARCH_CENTER;
			else if (index == 3)
				type = ObjectiveType.TRANSPORTATION_HUB;
			else if (index == 4)
				type = ObjectiveType.TRADE_TOWN;
			else if (index == 5)
				type = ObjectiveType.TOURISM;

			settlement.setObjective(type);
			
			update();
			
			//if (type != null && objectiveTypeCache != type) {
			//	objectiveTypeCache = type;
			//	objLabel.setText("Current Choice : " + objectiveTypeCache.toString());
			//}
		}
	}


	/*
	 * Display the initial system greeting and update the css style
	 */
	// 2016-10-31 Added update()
	@Override
	public void update() {
		Platform.runLater(() -> {
			if (settlement.getObjective() != null && objectiveTypeCache != settlement.getObjective()) {
				objectiveTypeCache = settlement.getObjective();
				if (choiceLabel != null) choiceLabel.setText(objectiveTypeCache.toString());
			}
			int theme = MainScene.getTheme();
			if (themeCache != theme) {
				themeCache = theme;
				
				stack.getStylesheets().clear();
				mainBox.getStylesheets().clear();
				headerBox.getStylesheets().clear();
				commitPane.getStylesheets().clear();
				//choiceBox.getStylesheets().clear();
				row1.getStylesheets().clear();
				row0.getStylesheets().clear();
				
				choiceBox.getStylesheets().clear();
				optionBox.getStylesheets().clear();
				
				headerLabel.getStylesheets().clear();
				choiceLabel.getStylesheets().clear();
				choiceHeader.getStylesheets().clear();
				
				commitButton.getStylesheets().clear();
				
				if (theme == 6) {
					String cssFile = "/fxui/css/snowBlue.css";
					String color = "-fx-border-style: 2px; " + "-fx-background-color: white;" + "-fx-border-color: white;"
							+ "-fx-background-radius: 2px;";
					setCSS(cssFile, color);
				} else if (theme == 0 || theme == 7) {
					String cssFile = "/fxui/css/nimrodskin.css";
					String color = "-fx-border-style: 2px; " + "-fx-background-color: #c1bf9d;" + "-fx-border-color: #c1bf9d;"
							+ "-fx-background-radius: 2px;";
					setCSS(cssFile, color);
				}
			}
		});
	}

	public void setCSS(String cssFile, String color) {
		headerLabel.getStylesheets().add(getClass().getResource(cssFile).toExternalForm());
		choiceLabel.getStylesheets().add(getClass().getResource(cssFile).toExternalForm());
		choiceHeader.getStylesheets().add(getClass().getResource(cssFile).toExternalForm());
		commitButton.getStylesheets().add(getClass().getResource(cssFile).toExternalForm());
		headerLabel.getStyleClass().add("label-medium");
		choiceLabel.getStyleClass().add("label-right");
		choiceHeader.getStyleClass().add("label-left");
		stack.setStyle(color);
		mainBox.setStyle(color);
		commitPane.setStyle(color);
		headerBox.setStyle(color);
		//choiceBox.setStyle(color);
		row0.setStyle(color);
		row1.setStyle(color);
		choiceBox.setStyle(color);
		optionBox.setStyle(color);
	}
	/**
     * Prepare object for garbage collection.
     */
    public void destroy() {
    	jfxpanel = null;
    	scene = null;
		stack = null;
		choiceLabel = null;
		toggleBtn = null;
		commitButton = null;
		mainBox = null;
		timer.stop();
		timer = null;
		commitButton = null;
    }
}