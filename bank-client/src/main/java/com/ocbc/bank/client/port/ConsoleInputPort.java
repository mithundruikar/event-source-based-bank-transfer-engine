package com.ocbc.bank.client.port;

import java.util.Scanner;

public class ConsoleInputPort implements UserInputPort {

    private Scanner in;
    public ConsoleInputPort() {
        this.in = new Scanner(System.in);
    }

    @Override
    public String getNextLine() {
        return in.nextLine();
    }

    @Override
    public void writeLine(String line) {
        System.out.println(line);
    }
}
