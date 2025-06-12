import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import java.util.ArrayList;
import java.util.List;

public class Main extends Application {
    private Emitter emitter;
    private Canvas canvas;
    private GraphicsContext g;
    private AnimationTimer timer;
    private BorderPane root;
    private long prev = System.nanoTime();
    private int frameCount = 0;
    private List<Integer> fpsList = new ArrayList<>();

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

        prev = System.nanoTime();
        frameCount = 0;
        fpsList.clear();
        startAnimation();
    }

    private void startAnimation() {
        timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                calcFps(now);
                g.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
                emitter.emit();
                emitter.update();
                emitter.render(g);
                if (emitter.finished()) {
                    double avgFps = 0;
                    timer.stop();
                    avgFps = fpsList.stream().mapToInt(Integer::intValue).average().orElse(0.0);
                    System.out.println("Average FPS: " + avgFps);
                    emitter.shutdown();
                }
            }
        };
        timer.start();
    }

    private void calcFps(long now) {
        if (now - prev > 1_000_000_000) {
            fpsList.add(frameCount);
            System.out.println("FPS: " + frameCount);
            prev = now;
            frameCount = 0;
        } else {
            frameCount++;
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}