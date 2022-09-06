package fr.irit.similarity;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Paths;
import java.util.List;
import java.util.Scanner;

public class ScibertSim {


    private static Process process;
    private static PrintWriter writer;
    private static Scanner reader;

    public static void start() throws IOException {
        ProcessBuilder processBuilder = new ProcessBuilder("/home/guilherme/PycharmProjects/scibertsim/venv/bin/python", "/home/guilherme/PycharmProjects/scibertsim/main.py");

        process = processBuilder.start();
        writer = new PrintWriter(process.getOutputStream());

        reader = new Scanner(process.getInputStream());

    }


    public static double getSim(String s1, String s2){
        writer.println(String.format("{\"cmd\":\"sim\", \"s1\":\"%s\", \"s2\":\"%s\"}", s1, s2));
        writer.flush();

        return Double.parseDouble(reader.nextLine());
    }

    public static void stop() throws InterruptedException {
        writer.println("{cmd:\"end\"}");
        writer.flush();
        int exitCode = process.waitFor();
    }
}
