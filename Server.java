/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package s2122.hw3;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveAction;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Multi-threaded server program that multiplies matrices using fork-join
 * framework.
 *
 * @author MJY
 */
public class Server implements Runnable {

    private Socket socket;

    public Server() {
    }

    public Server(Socket socket) {
        this.socket = socket;
    }

    /**
     * Driver function. Start this server at port 33333.
     */
    public static void main(String[] args) {
        start(33333);
    }

    /**
     * Start matrix server at the specified port. It should accept and handle
     * multiple client requests concurrently.
     *
     * @param port port number listened by the server
     */
    public static void start(int port) {
        ExecutorService pool = Executors.newCachedThreadPool();

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            // your implementation here
            pool.execute(new Server56853140(serverSocket.accept()));

            //new Thread(new Server56853140(serverSocket.accept())).start();
            //t.start();
        } catch (IOException ex) {
            Logger.getLogger(Server56853140.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    /**
     * Handle a matrix client request. It reads two matrices from socket,
     * compute their product, and then send the product matrix back to the
     * client.
     */
    @Override
    public void run() {

        // your implementation here
        try {
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            boolean dataA = true;
            String inputLine;
            ArrayList<long[]> matrixAL = new ArrayList();
            ArrayList<long[]> matrixBL = new ArrayList();
            int line = 0;
            dataA = true;
            while ((inputLine = in.readLine()) != null ) {
                if(inputLine.equals("")){
                    break;
                }
                
                if (inputLine.equals("end")) {
                    dataA = false;
                    line = 0;
                } else {
                    String fig[] = inputLine.split("]");
                    
                    List<Long> listA = new ArrayList();
                    List<Long> listB = new ArrayList();

                    for (int j = 0; j < fig.length; j++) {
                        fig[j] = fig[j].replace(" ", "").replace("[", "");
                        if (dataA) {
                            listA.add(Long.parseLong(fig[j]));
                        } else { 
                            listB.add(Long.parseLong(fig[j]));
                        }

                    }
                    if (dataA) {
                        matrixAL.add(listA.stream().mapToLong(Long::new).toArray());
                    } else {
                        matrixBL.add(listB.stream().mapToLong(Long::new).toArray());
                    }
                    
                    line++;
                }
            }
            long[][] matAL = new long[matrixAL.size()][matrixAL.get(0).length];
            long[][] matBL = new long[matrixBL.size()][matrixBL.get(0).length];
            
            for (int i = 0; i < matrixAL.size(); i++) {
                matAL[i] = matrixAL.get(i);
            }
            for (int i = 0; i < matrixBL.size(); i++) {
                matBL[i] = matrixBL.get(i);
            }
            Matrix matA = new Matrix(matAL);
            Matrix matB = new Matrix(matBL);

            Matrix solution = multiThreadMultiply(matA, matB);
            out.println(solution);
            out.close();
            in.close();
            socket.close();

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    /**
     * Compute A x B using fork-join framework.
     *
     * @param matA matrix A
     * @param matB matrix B
     * @return the matrix product of AxB
     */
    public static Matrix multiThreadMultiply(Matrix matA, Matrix matB) {

        Matrix product = null;
        // your implementation here
        long[][] productL = new long[matA.row()][matB.col()];
        RecursiveAction task = new ParallelMultiply(matA, matB, productL);
        ForkJoinPool pool = new ForkJoinPool();
        pool.invoke(task);
        product = new Matrix(productL);
        return product;
    }
}

/**
 * Design a recursive and resultless ForkJoinTask. It splits the matrix
 * multiplication into multiple tasks to be executed in parallel.
 *
 */
class ParallelMultiply extends RecursiveAction {

    // your implementation here
    private Matrix matA;
    private Matrix matB;
    //private Matrix product;
    private long[][] productL;

    ParallelMultiply(Matrix matA, Matrix matB, long[][] productL){
        this.matA = matA;
        this.matB = matB;
        this.productL = productL;
    }
    @Override
    public void compute() {
      List<RecursiveAction> tasks = new ArrayList<>();
      for (int i = 0; i < productL.length; i++)
        for (int j = 0; j < productL[0].length; j++)
          tasks.add(new MultiplyOneRow(i, j));
      
      invokeAll(tasks);
    }
    
    public class MultiplyOneRow extends RecursiveAction {
      int i;
      int j;
      
      public MultiplyOneRow(int i, int j) {
        this.i = i;
        this.j = j;
      }
      
      @Override
      public void compute() {
        productL[i][j] = 0;
        for (int k = 0; k < matA.col(); k++)
          productL[i][j] += matA.at(i, k) * matB.at(k, j);
      }
    }

}
