import javafx.scene.canvas.GraphicsContext;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Emitter {
    private final double x, y;
    private final double emitRate;
    private final int maxPart;
    private final String emitType;
    private final double width, height;
    private boolean hasEmitted = false;
    private int totalEmitted = 0;
    private final List<Particle> particles;
    private final int numThreads;

    public Emitter(double x, double y, double emitRate, int maxPart, String emitType, double width, double height) {
        this.x = x; this.y = y;
        this.emitRate = emitRate;
        this.maxPart = maxPart;
        this.emitType = emitType;
        this.width = width; this.height = height;
        this.particles = Collections.synchronizedList(new ArrayList<>());
        this.numThreads = Runtime.getRuntime().availableProcessors();
    }

    public void emit() {
        int toEmit = 0;
        if ("continuous".equals(emitType)) { // Continuous emission
            synchronized (particles) {
                toEmit = (int) Math.min(emitRate, maxPart - totalEmitted);
            }
        } else if ("burst".equals(emitType)) { // Burst emision
            if (!hasEmitted) {
                toEmit = maxPart;
            }
        }
        if (toEmit <= 0) return;

        //parallell emission
        int chunk = (toEmit + numThreads - 1) / numThreads;
        List<Thread> threads = new ArrayList<>();
        for (int t = 0; t < numThreads; t++) {
            final int start = t * chunk;
            final int end = Math.min(toEmit, start + chunk);
            if (start >= end) break;

            Thread th = new Thread(() -> {
                List<Particle> local = new ArrayList<>(end - start);
                for (int i = start; i < end; i++) {
                    local.add(new Particle(x, y,
                            Math.random() * 2 - 1,
                            Math.random() * 2 - 1,
                            15.0, width, height));
                }
                synchronized (particles) {
                    particles.addAll(local);
                    totalEmitted += local.size();
                }
            }, "EmitterEmit-" + t);
            threads.add(th);
            th.start();
        }
        for (Thread th : threads) {
            try { th.join(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        }
        if ("burst".equals(emitType)) {
            hasEmitted = true;
        }
    }

    // update particles (apply physics and check colisions)
    public void update() {
        List<Particle> snapshot;
        synchronized (particles) {
            snapshot = new ArrayList<>(particles);
        }
        int size = snapshot.size();
        int chunk = (size + numThreads - 1) / numThreads;

        // parallel update
        List<Thread> workers = new ArrayList<>();
        for (int t = 0; t < numThreads; t++) {
            final int start = t * chunk;
            final int end = Math.min(size, start + chunk);
            if (start >= end) break;
            Thread upd = new Thread(() -> {
                for (int i = start; i < end; i++) {
                    snapshot.get(i).update();
                }
            }, "UpdateWorker-" + t);
            workers.add(upd);
            upd.start();
        }
        for (Thread w : workers) {
            try { w.join(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        }

        // parallel collision
        workers.clear();
        for (int t = 0; t < numThreads; t++) {
            final int start = t * chunk;
            final int end = Math.min(size, start + chunk);
            if (start >= end) break;
            Thread col = new Thread(() -> {
                for (int i = start; i < end; i++) {
                    Particle p1 = snapshot.get(i);
                    for (int j = i + 1; j < size; j++) {
                        p1.colision(snapshot.get(j));
                    }
                }
            }, "CollideWorker-" + t);
            workers.add(col);
            col.start();
        }
        for (Thread w : workers) {
            try { w.join(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        }

        //remove
        synchronized (particles) {
            particles.removeIf(p -> !p.isAlive());
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
        if ("continuous".equals(emitType)) {
            return totalEmitted >= maxPart && particles.isEmpty();
        } else {
            return hasEmitted && particles.isEmpty();
        }
    }
    public int getParticlesCount() {
        synchronized (particles) {
            return particles.size();
        }
    }
}
