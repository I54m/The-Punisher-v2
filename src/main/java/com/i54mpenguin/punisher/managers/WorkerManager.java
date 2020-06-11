package com.i54mpenguin.punisher.managers;

import com.i54mpenguin.punisher.PunisherPlugin;
import com.i54mpenguin.punisher.exceptions.ManagerNotStartedException;
import com.i54mpenguin.punisher.handlers.ErrorHandler;
import lombok.Getter;
import net.md_5.bungee.api.ChatColor;

import java.util.ArrayList;

public class WorkerManager {

    @Getter
    private static final WorkerManager INSTANCE = new WorkerManager();
    private final ArrayList<Worker> workers = new ArrayList<>();
    private boolean locked = true;
    private Thread mainThread;
    private final PunisherPlugin plugin = PunisherPlugin.getInstance();

    private WorkerManager() {
    }

    public synchronized void start() {
        try {
            if (!locked)
                throw new Exception("Worker Manager Already started!");
        } catch (Exception e) {
            // TODO: 11/06/2020 need to handle this error
            e.printStackTrace();
            return;
        }
        mainThread = Thread.currentThread();
        locked = false;
        plugin.getLogger().info(plugin.getPrefix() + ChatColor.GREEN + "Started Worker Manager!");
    }

    public boolean isStarted() {
        return !locked;
    }

    public synchronized void stop() {
        try {
            if (locked)
                throw new ManagerNotStartedException(this.getClass());
        } catch (ManagerNotStartedException mnse) {
            ErrorHandler.getINSTANCE().log(mnse);
            return;
        }
        locked = true;
        try {
            if (!workers.isEmpty()){
                plugin.getLogger().info(plugin.getPrefix() + ChatColor.GREEN + "Pausing main thread while workers finish up!");
                mainThread.wait();
            }
        } catch (InterruptedException e) {
            plugin.getLogger().severe(plugin.getPrefix() + ChatColor.RED + "Error: main thread was interrupted while waiting for workers to finish!");
            plugin.getLogger().severe(plugin.getPrefix() + ChatColor.RED + "Interrupting workers, this may cause data loss!!");
            PunisherPlugin.getLOGS().severe("Error: main thread was interrupted while waiting for workers to finish!");
            PunisherPlugin.getLOGS().severe("Interrupting workers, this may cause data loss!!");
            for (Worker worker : workers) {
                plugin.getLogger().severe(plugin.getPrefix() + ChatColor.RED + "Interrupting " + worker.getName());
                PunisherPlugin.getLOGS().severe("Interrupting " + worker.getName());
                worker.interrupt();
            }
        }
        workers.clear();
    }

    public synchronized void runWorker(Worker worker) {
        try {
            if (locked)
                throw new ManagerNotStartedException(this.getClass());
        } catch (ManagerNotStartedException mnse) {
            ErrorHandler.getINSTANCE().log(mnse);
            return;
        }
        worker.setName("The-Punisher - Worker Thread #" + (workers.isEmpty() ? 1 : workers.size() + 1));
        workers.add(worker);
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
            } catch (Exception e){
                // TODO: 19/05/2020 error handler thread error
                status = Status.FINISHED;
                WorkerManager.getINSTANCE().finishedWorker(this);
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
