package com.example.vintify;

import javax.swing.table.TableRowSorter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class Koneksi {
    private static Connection conn;

    public static Connection getConnect() {
        if (conn == null) {
            try {
                String url = "jdbc:mysql://localhost/ db_vintify";
                String user = "root";
                String pass = "";
                conn = DriverManager.getConnection(url, user, pass);
                System.out.println("Koneksi ke database vintify berhasil!");
            } catch (SQLException er) {
                System.out.println("Gagal membuat koneksi ke database. Periksa code anda secara teliti.");
            }
        }

        return conn;
    }

    public static void main(String[] args) {
        Koneksi k = new Koneksi();
        k.getConnect();
    }
}
