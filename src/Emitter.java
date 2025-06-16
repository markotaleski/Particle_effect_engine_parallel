import javafx.scene.canvas.GraphicsContext;
import java.util.*;
import java.util.concurrent.*;

public class Emitter {
    private final double x, y;
    private final double emitRate;
    private final int maxPart;
    private final String emitType;
    private final double width, height;
    private final List<Particle> particles = Collections.synchronizedList(new ArrayList<>());
    private boolean hasEmitted = false;
    private int totalEmitted = 0;

    // Thread‐pool setup
    private final int numThreads ;
    private final ExecutorService pool;
    private final List<Callable<Void>> tasks = new ArrayList<>();

    public Emitter(double x, double y, double emitRate, int maxPart,
                           String emitType, double width, double height) {
        this.x = x; this.y = y;
        this.emitRate = emitRate;
        this.maxPart = maxPart;
        this.emitType = emitType;
        this.width = width; this.height = height;
        this.numThreads  = Math.max(1, Runtime.getRuntime().availableProcessors() - 1);
        this.pool = Executors.newFixedThreadPool(numThreads );
    }


    public void shutdown() {
        pool.shutdownNow();
    }


    public void emit() {
        int toEmit = 0;
        synchronized (particles) {
            if ("continuous".equals(emitType)) { // Continuous emission
                toEmit = (int)Math.min(emitRate, maxPart - totalEmitted);
                totalEmitted += toEmit;
            } else if ("burst".equals(emitType) && !hasEmitted) {  // Burst emision
                toEmit = maxPart;
                hasEmitted = true;
            }
        }
        if (toEmit <= 0) return;

        //parallell emission
        int chunk = (toEmit + numThreads  - 1) / numThreads ;
        tasks.clear();
        for (int t = 0; t < numThreads ; t++) {
            final int start = t * chunk;
            final int end = Math.min(toEmit, start + chunk);
            if (start >= end) break;

            //creation of tasks
            tasks.add(() -> {
                List<Particle> local = new ArrayList<>(end - start);
                for (int i = start; i < end; i++) {
                    local.add(new Particle(x, y,
                            Math.random() * 2 - 1,
                            Math.random() * 2 - 1,
                            15.0, width, height));
                }
                //merging
                synchronized (particles) {
                    particles.addAll(local);
                }
                return null;
            });
        }
        //invoking tasks
        try {
            pool.invokeAll(tasks);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    // update particles (apply physics and check colisions)
    public void update() {
        List<Particle> snapshot;
        synchronized (particles) {
            snapshot = new ArrayList<>(particles);
        }
        int size = snapshot.size();
        if (size == 0) {
            synchronized (particles) {
                particles.clear();
            }
            return;
        }

        int chunk = (size + numThreads  - 1) / numThreads ;

        // parallel update
        tasks.clear();
        for (int t = 0; t < numThreads ; t++) {
            final int start = t * chunk;
            final int end   = Math.min(size, start + chunk);
            if (start >= end) break;
            tasks.add(() -> {
                for (int i = start; i < end; i++) {
                    snapshot.get(i).update();
                }
                return null;
            });
        }
        try { pool.invokeAll(tasks); }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); }

        // parallel collision
        tasks.clear();
        for (int t = 0; t < numThreads ; t++) {
            final int start = t * chunk;
            final int end   = Math.min(size, start + chunk);
            if (start >= end) break;
            tasks.add(() -> {
                for (int i = start; i < end; i++) {
                    Particle p1 = snapshot.get(i);
                    for (int j = i + 1; j < size; j++) {
                        Particle p2 = snapshot.get(j);
                        if (Math.abs(p1.getX() - p2.getX()) < p1.getRadius() * 2 &&
                                Math.abs(p1.getY() - p2.getY()) < p1.getRadius() * 2) {
                            p1.colision(p2);
                        }
                    }
                }
                return null;
            });
        }
        try { pool.invokeAll(tasks); }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); }

        // parallel remove
        tasks.clear();
        List<List<Particle>> survivors = Collections.synchronizedList(new ArrayList<>());
        for (int t = 0; t < numThreads ; t++) {
            final int start = t * chunk;
            final int end   = Math.min(size, start + chunk);
            if (start >= end) break;
            tasks.add(() -> {
                List<Particle> localAlive = new ArrayList<>();
                for (int i = start; i < end; i++) {
                    if (snapshot.get(i).isAlive()) {
                        localAlive.add(snapshot.get(i));
                    }
                }
                survivors.add(localAlive);
                return null;
            });
        }
        try { pool.invokeAll(tasks); }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); }

        //merging
        synchronized (particles) {
            particles.clear();
            for (List<Particle> chunkList : survivors) {
                particles.addAll(chunkList);
            }
        }
    }

    public void render(GraphicsContext g) {
        synchronized (particles) {
            for (Particle p : particles) {
                p.render(g);
            }
        }
    }

    public boolean finished() {
        synchronized (particles) {
            if ("continuous".equals(emitType)) {
                return totalEmitted == maxPart && particles.isEmpty();
            } else {
                return hasEmitted && particles.isEmpty();
            }
        }
    }
}
