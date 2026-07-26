package concurrency;

public class CallbackStyleExample {

    public static void main(String[] args) {
        int x = 1337;
        Result result = new Result();

        f(x, y -> {
            synchronized (result) {
                result.left = y;
                result.leftReady = true;

                if (result.rightReady) {
                    System.out.println(result.left + result.right);
                }
            }
        });

        g(x, z -> {
            synchronized (result) {
                result.right = z;
                result.rightReady = true;

                if (result.leftReady) {
                    System.out.println(result.left + result.right);
                }
            }
        });
    }

    static void f(int x, Callback callback) {
        new Thread(() -> {
            sleep(1000);
            callback.call(x * 2);
        }).start();
    }

    static void g(int x, Callback callback) {
        new Thread(() -> {
            sleep(500);
            callback.call(x + 10);
        }).start();
    }

    static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @FunctionalInterface
    interface Callback {
        void call(int value);
    }

    private static class Result {
        int left;
        int right;
        boolean leftReady;
        boolean rightReady;
    }
}
