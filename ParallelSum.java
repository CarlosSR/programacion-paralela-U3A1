import java.util.Random;

public class ParallelSum {

    private static final int THREAD_COUNT = 4;
    private static final int ARRAY_SIZE = 1_000_000_000;
    private static final int MAX_VALUE = 100;
    private static final long SEED = 42L;

    public static void main(String[] args) throws InterruptedException {
        int[] data = generateArray(ARRAY_SIZE, MAX_VALUE, SEED);

        System.out.printf("Arreglo generado: %d elementos%n", ARRAY_SIZE);
        System.out.printf("Hilos a utilizar: %d%n%n", THREAD_COUNT);

        long seqStart = System.nanoTime();
        long seqTotal = sumSequentially(data);
        long seqNanos = System.nanoTime() - seqStart;

        long parStart = System.nanoTime();
        long[] partials = sumInParallel(data, THREAD_COUNT);
        long parTotal = reduce(partials);
        long parNanos = System.nanoTime() - parStart;

        printResult("SOLUCIÓN SECUENCIAL", seqTotal, seqNanos);
        printResult("SOLUCIÓN PARALELA", parTotal, parNanos);
        printComparison(seqTotal, seqNanos, parTotal, parNanos);
        printBlockBreakdown(data.length, partials);
    }

    private static int[] generateArray(int size, int maxValue, long seed) {
        int[] data = new int[size];
        Random random = new Random(seed);
        for (int i = 0; i < size; i++) {
            data[i] = random.nextInt(maxValue);
        }
        return data;
    }

    private static long sumSequentially(int[] data) {
        long sum = 0;
        for (int value : data) {
            sum += value;
        }
        return sum;
    }

    private static long[] sumInParallel(int[] data, int threadCount) throws InterruptedException {
        // tamaño base de cada bloque
        int baseLength = data.length / threadCount;   

        // elementos sobrantes si no es divisible
        int remainder = data.length % threadCount;

        // una celda por hilo, sin colisiones
        long[] partials = new long[threadCount];      
        
        // referencias para luego hacer join()
        Thread[] threads = new Thread[threadCount];   

        int start = 0;
        for (int i = 0; i < threadCount; i++) {
            // los primeros hilos absorben el resto
            int length = baseLength + (i < remainder ? 1 : 0); 
            // copia local: la lambda solo captura variables finales
            int from = start;                          
            // fin exclusivo del rango
            int to = start + length;
            // copia local del índice de este hilo
            int index = i;                             

            // sumar el rango y guardar el parcial 
            // nombre del hilo (útil para debug)
            threads[i] = new Thread(() -> partials[index] = sumRange(data, from, to), "SumThread-" + i);

            // arranca el hilo. Se ejecuta la lambda
            threads[i].start();                        
            // el siguiente bloque empieza donde terminó este
            start = to;                                
        }

        // barrera: espera a que todos terminen
        awaitAll(threads);
        // resultados listos para reducir
        return partials;
}

    private static long sumRange(int[] data, int from, int to) {
        long sum = 0;
        for (int i = from; i < to; i++) {
            sum += data[i];
        }
        return sum;
    }

    private static void awaitAll(Thread[] threads) throws InterruptedException {
        for (Thread thread : threads) {
            thread.join();
        }
    }

    private static long reduce(long[] partials) {
        long total = 0;
        for (long partial : partials) {
            total += partial;
        }
        return total;
    }

    private static void printResult(String title, long total, long nanos) {
        double milliseconds = nanos / 1_000_000.0;
        System.out.printf("=== %s ===%n", title);
        System.out.printf("Suma total: %d%n", total);
        System.out.printf("Tiempo:     %.3f ms%n%n", milliseconds);
    }

    private static void printComparison(long seqTotal, long seqNanos, long parTotal, long parNanos) {
        double seqMs = seqNanos / 1_000_000.0;
        double parMs = parNanos / 1_000_000.0;

        System.out.println("=== RESULTADOS ===");
        System.out.printf("¿Coinciden ambas sumas? %s%n",
                seqTotal == parTotal ? "SI" : "NO");
        System.out.printf("Speedup (sec/par): %.2fx%n", seqMs / parMs);
    }

    private static void printBlockBreakdown(int totalSize, long[] partials) {
        System.out.println("\nDetalle de bloques:");
        int baseLength = totalSize / ParallelSum.THREAD_COUNT;
        int remainder = totalSize % ParallelSum.THREAD_COUNT;
        int position = 0;
        for (int i = 0; i < ParallelSum.THREAD_COUNT; i++) {
            int length = baseLength + (i < remainder ? 1 : 0);
            System.out.printf("  Hilo %d -> [%d, %d)  longitud=%d  parcial=%d%n",
                    i, position, position + length, length, partials[i]);
            position += length;
        }
    }
}
