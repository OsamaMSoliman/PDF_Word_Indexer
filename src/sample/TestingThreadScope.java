package sample;

public class TestingThreadScope implements Runnable {

    //shared
    private static int s;
    //not shared
    private int noS;

    @Override
    public void run() {
        for (int i = 0; i < 10; i++) {
            System.out.println(Thread.currentThread().getName() + " noS = " + noS++);
            System.out.println(Thread.currentThread().getName() + " s = " + s++);
        }
    }

    public static void main(String[] args) {
        Thread[] ts = new Thread[3];
        for (int i = 0; i < ts.length; i++) {
            ts[i] = new Thread(new TestingThreadScope());
            ts[i].start();
        }
    }
}
