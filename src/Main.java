import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

public class Main extends Application {
    private Emitter emitter;
    private Canvas canvas;
    private GraphicsContext g;
    private AnimationTimer timer;
    private BorderPane root;

    @Override
    public void start(Stage primaryStage) {
        root = new BorderPane();
        canvas = new Canvas(800, 600);
        g = canvas.getGraphicsContext2D();
        root.setCenter(canvas);

        Input input = new Input(this);
        root.setRight(input.inputs());

        Scene scene = new Scene(root, 1000, 600);
        primaryStage.setScene(scene);
        primaryStage.setTitle("Particle Effect Engine");
        primaryStage.show();

        build(200, 800, 600, 400, 300, "continuous");
    }

    public void build(int nParticles, int width, int height,
                      double emitterX, double emitterY, String emitType) {
        if(timer != null) {
            timer.stop();
        }

        canvas.setWidth(width);
        canvas.setHeight(height);
        emitter = new Emitter(emitterX, emitterY, 5, nParticles, emitType, width, height);
        startAnimation();
    }

    private void startAnimation() {
        timer = new AnimationTimer() {
            private long startTime = System.nanoTime();  // Store the start time of the animation
            private boolean isStarted = false;

            @Override
            public void handle(long now) {
                if (!isStarted) {
                    startTime = now;
                    isStarted = true;
                }
                g.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
                emitter.emit();
                emitter.update();
                emitter.render(g);
                if (emitter.finished()) {
                    long endTime = System.nanoTime();
                    double totalTimeInSeconds = (endTime - startTime) / 1_000_000_000.0;
                    System.out.println("Total animation time: " + totalTimeInSeconds + " seconds");
                    stop();
                }
            }
        };
        timer.start();
    }

    public static void main(String[] args) {
        launch(args);
    }
}