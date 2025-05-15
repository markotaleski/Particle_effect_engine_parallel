import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

public class Input {
    private Main main;  // Reference to the Main class
    private TextField particleField;
    private TextField widthField;
    private TextField heightField;
    private TextField emitterXField;
    private TextField emitterYField;
    private ChoiceBox<String> emitTypeChoice;
    private Button startButton;

    public Input(Main main) {
        this.main = main;  // reference of Main class
    }

    public VBox inputs() {

        particleField = new TextField("200"); // input for num of particles
        particleField.setPromptText("Number of Particles");

        widthField = new TextField("800"); // input for width
        widthField.setPromptText("Window Width");

        heightField = new TextField("600"); // input for height
        heightField.setPromptText("Window Height");

        emitterXField = new TextField("400"); // input for x emitter
        emitterXField.setPromptText("Emitter X Position");

        emitterYField = new TextField("300"); // input for y emitter
        emitterYField.setPromptText("Emitter Y Position");

        emitTypeChoice = new ChoiceBox<>(); // emission type
        emitTypeChoice.getItems().addAll("continuous", "burst");
        emitTypeChoice.setValue("continuous");

        startButton = new Button("Start Simulation");


        startButton.setOnAction(event -> {
            try {
                int nParticles = Integer.parseInt(particleField.getText());
                int width = Integer.parseInt(widthField.getText());
                int height = Integer.parseInt(heightField.getText());
                double emitterX = Double.parseDouble(emitterXField.getText());
                double emitterY = Double.parseDouble(emitterYField.getText());
                String emitType = emitTypeChoice.getValue();
                // start simulation
                main.build(nParticles, width, height, emitterX, emitterY, emitType);
            } catch (NumberFormatException e) {
                // error in case of wrong
                System.out.println("Invalid input, please enter valid numbers");

            }
        });

        // Box layout
        VBox layout = new VBox(10, particleField, widthField, heightField, emitterXField, emitterYField, emitTypeChoice, startButton);
        layout.setPrefWidth(200);
        return layout;
    }
}

