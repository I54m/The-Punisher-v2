package com.i54m.punisher.managers;

import com.i54m.punisher.PunisherPlugin;
import com.i54m.punisher.exceptions.ManagerNotStartedException;
import com.i54m.punisher.exceptions.WorkerException;
import lombok.Getter;
import net.md_5.bungee.api.ChatColor;

import java.util.ArrayList;

public class WorkerManager implements Manager {

    @Getter
    private static final WorkerManager INSTANCE = new WorkerManager();
    private final ArrayList<Worker> workers = new ArrayList<>();
    private boolean locked = true;
    private Thread mainThread;

    private WorkerManager() {
    }

    @Override
    public synchronized void start() {
        if (!locked) {
            ERROR_HANDLER.log(new Exception("Worker Manager Already started!"));
            return;
        }
        mainThread = Thread.currentThread();
        locked = false;
        PLUGIN.getLogger().info(ChatColor.GREEN + "Started Worker Manager!");
    }

    @Override
    public boolean isStarted() {
        return !locked;
    }

    @Override
    public synchronized void stop() {
        if (locked) {
            ERROR_HANDLER.log(new ManagerNotStartedException(this.getClass()));
            return;
        }
        locked = true;
        try {
            if (!workers.isEmpty()) {
                PLUGIN.getLogger().info(ChatColor.GREEN + "Pausing main thread while workers finish up!");
                mainThread.wait();
            }
        } catch (InterruptedException e) {
            PLUGIN.getLogger().severe(ChatColor.RED + "Error: main thread was interrupted while waiting for workers to finish!");
            PLUGIN.getLogger().severe(ChatColor.RED + "Interrupting workers, this may cause data loss!!");
            PunisherPlugin.getLOGS().severe("Error: main thread was interrupted while waiting for workers to finish!");
            PunisherPlugin.getLOGS().severe("Interrupting workers, this may cause data loss!!");
            for (Worker worker : workers) {
                PLUGIN.getLogger().severe(ChatColor.RED + "Interrupting " + worker.getName());
                PunisherPlugin.getLOGS().severe("Interrupting " + worker.getName());
                worker.interrupt();
            }
        }
        workers.clear();
    }

    public synchronized void runWorker(Worker worker) {
        if (locked) {
            ERROR_HANDLER.log(new ManagerNotStartedException(this.getClass()));
            return;
        }
        workers.add(worker);
        worker.setName("The-Punisher - Worker Thread #" + (workers.indexOf(worker) + 1));
        worker.start();
    }

    private synchronized void finishedWorker(Worker worker) {
        if (worker.getStatus() == Worker.Status.FINISHED)
            workers.remove(worker);
        if (locked && workers.isEmpty())
            mainThread.notifyAll();
    }


    public static class Worker extends Thread {

        private final Runnable runnable;
        private Status status;

        public Worker(Runnable runnable) {
            this.runnable = runnable;
            this.status = Status.CREATED;
        }

        @Override
        public void run() {
            status = Status.WORKING;
            try {
                runnable.run();
            } catch (Exception e) {
                ERROR_HANDLER.log(new WorkerException(this, e));
            }
            status = Status.FINISHED;
            WorkerManager.getINSTANCE().finishedWorker(this);
        }

        public WorkerManager.Worker.Status getStatus() {
            return this.status;
        }


        public enum Status {
            CREATED, WORKING, FINISHED
        }
    }

}
