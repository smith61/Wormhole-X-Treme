/*
 * Wormhole X-Treme Plugin for Bukkit
 * Copyright (C) 2011 Lycano <https://github.com/lycano/Wormhole-X-Treme/>
 *
 * Copyright (C) 2011 Ben Echols
 *                    Dean Bailey
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.luricos.bukkit.WormholeXTreme.Wormhole.utils;

import de.luricos.bukkit.WormholeXTreme.Wormhole.WormholeXTreme;
import de.luricos.bukkit.WormholeXTreme.Wormhole.model.StargateDBManager;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.security.CodeSource;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * WormholeXTreme DBUpdateUtil.
 * 
 * @author Ben Echols (Lologarithm)
 */
public class DBUpdateUtil {

    /** The sql_con. */
    private static Connection sql_con;

    /**
     * Gets the count db files.
     * 
     * @return the count db files
     */
    private static int getCountDBFiles() {
        final CodeSource src = WormholeXTreme.class.getProtectionDomain().getCodeSource();
        final URL jar = src.getLocation();
        int count = 0;
        ZipInputStream zis = null;
        try {
            zis = new ZipInputStream(jar.openStream());
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.getName().contains("db_create_")) {
                    count++;
                }
            }
            zis.close();
        } catch (final IOException e) {
            WXTLogger.prettyLog(Level.SEVERE, false, "Unable to open jar file to read SQL Update commands: " + e.getMessage());
        } finally {
            try {
                zis.close();
            } catch (final IOException e) {
                WXTLogger.prettyLog(Level.FINE, false, e.getMessage());
            }
        }
        return count;
    }

    /**
     * Gets the current version.
     * 
     * @return the current version
     */
    private static int getCurrentVersion() {
        int ver = 0;
        Statement stmt = null;
        ResultSet rs = null;
        try {
            stmt = sql_con.createStatement();
            rs = stmt.executeQuery("SELECT MAX(Version) as ver FROM VersionInfo");
            if (rs.next()) {
                ver = rs.getInt("ver");
            }
            stmt.close();
        } catch (final SQLException e) {
            WXTLogger.prettyLog(Level.WARNING, false, "Failed to load WormholeXTremeDB version info, defaulting to 0.");
            WXTLogger.prettyLog(Level.WARNING, false, "If this is your first time running this plugin, you can ignore this error.");
            return 0;
        } finally {
            try {
                if (rs != null) {
                    rs.close();
                }
            } catch (final SQLException e) {
                WXTLogger.prettyLog(Level.FINE, false, e.getMessage());
            }
            try {
                if (stmt != null) {
                    stmt.close();
                }
            } catch (final SQLException e) {
                WXTLogger.prettyLog(Level.FINE, false, e.getMessage());
            }

        }
        return ver;
    }

    /**
     * Read text from jar.
     * 
     * @param s
     *            the s
     * @return the array list
     */
    private static ArrayList<String> readTextFromJar(final String s) {
        InputStream is = null;
        BufferedReader br = null;
        String line;
        final ArrayList<String> list = new ArrayList<String>();

        try {
            is = WormholeXTreme.class.getResourceAsStream(s);
            br = new BufferedReader(new InputStreamReader(is));
            while (null != (line = br.readLine())) {
                list.add(line);
            }
        } catch (final Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (br != null) {
                    br.close();
                }
                if (is != null) {
                    is.close();
                }
            } catch (final IOException e) {
                e.printStackTrace();
            }
        }
        return list;
    }

    /**
     * Update db.
     * 
     * @return true, if successful
     */
    public static boolean updateDB() {
        final File dir = new File("plugins/WormholeXTremeDB/");
        final File dest_dir = new File("plugins/WormholeXTreme/WormholeXTremeDB/");

        File oldFileName = new File(dest_dir.getPath() + File.separator + "WormholeXTremeDB");
        File newFileName = new File(dest_dir.getPath() + File.separator + "WormholeXTreme.sqlite");

        if (!dest_dir.exists()) {
            try {
                dest_dir.mkdir();
            } catch (final Exception e) {
                WXTLogger.prettyLog(Level.SEVERE, false, "Unable to make directory: " + e.getMessage());
            }
        }

        if (dir.exists() && dir.isDirectory()) {
            WXTLogger.prettyLog(Level.WARNING, false, "Old Database found, moving directory.");
            final File[] files = dir.listFiles();
            for (File f : files) {
                try {
                    f.renameTo(new File(dest_dir, f.getName()));
                } catch (final Exception e) {
                    WXTLogger.prettyLog(Level.SEVERE, false, "Unable to rename files: " + e.getMessage());
                }
            }

            try {
                dir.delete();
            } catch (final Exception e) {
                WXTLogger.prettyLog(Level.SEVERE, false, "Unable to delete directory: " + e.getMessage());
                return false;
            }
        }
        
        if (oldFileName.isFile()) {
            WXTLogger.prettyLog(Level.WARNING, false, "Old Database File found. Performing Update after failsafe check.");
            if (newFileName.isFile()) {
                WXTLogger.prettyLog(Level.SEVERE, false, oldFileName.getName() +" and " + newFileName.getName() + " found both! Deleting failed during update. Please remove the correct file by hand (should be 0 KB).");
                return false;
            }

            try {
                if (!oldFileName.renameTo(newFileName))
                    throw new Exception("Check your database directory!");
                
                WXTLogger.prettyLog(Level.INFO, false, "Successfully moved old Database to new Database.");
            } catch (Exception e) {
                WXTLogger.prettyLog(Level.SEVERE, false, "Unable to rename or delete oldFile. This is a serious problem! " + e.getMessage());
                return false;
            }
        }

        
        
        // Start Smith_61 Edit
        
        
//        try {
//            Class.forName("org.sqlite.JDBC");
//        } catch (final ClassNotFoundException e) {
//            WXTLogger.prettyLog(Level.FINE, false, e.getMessage());
//            return false;
//        }
//
//        try {
//            sql_con = DriverManager.getConnection("jdbc:sqlite:./plugins/WormholeXTreme/WormholeXTremeDB/WormholeXTreme.sqlite", "sa", "");
//            sql_con.setAutoCommit(true);
//            sql_con.prepareStatement("PRAGMA journal_mode = TRUNCATE;VACUUM;").executeQuery().close();
//        } catch (final SQLException e) {
//            WXTLogger.prettyLog(Level.FINE, false, e.getMessage());
//            return false;
//        }
        
        DBUpdateUtil.sql_con = StargateDBManager.getConnection();
        if(DBUpdateUtil.sql_con == null) {
        	return false;
        }

        
        // End Smith_61 Edit
        
        
        
        final int version = getCurrentVersion();
        final int count = getCountDBFiles();

        updateDB(version, count);

        return true;
    }

    /**
     * Update db.
     * 
     * @param version
     *            the version
     * @param count
     *            the count
     */
    private static void updateDB(final int version, final int count) {
        if (count > version) {
            boolean success = true;
            Statement stmt = null;
            try {
                stmt = sql_con.createStatement();
                for (int i = (version + 1); i <= count; i++) {
                    StringBuilder sb = new StringBuilder();
                    final ArrayList<String> lines = readTextFromJar("/sql_commands/db_create_" + i);

                    for (final String line : lines) {
                        if (!line.startsWith("#") && !line.startsWith("--")) {
                            sb.append(line);
                        }

                        if (line.endsWith(";") && !line.startsWith("#")) {
                            try {
                                stmt.executeUpdate(sb.toString());
                                //System.out.println("StargatesDB updated:" + rs + " : " + sb.toString());
                            } catch (final SQLException sql_e) {
                                final int code = sql_e.getErrorCode();
                                if ((code == -27) || (code == -21)) {
                                    WXTLogger.prettyLog(Level.WARNING, false, "(" + code + ")Continuing after Error:" + sql_e);
                                } else {
                                    WXTLogger.prettyLog(Level.SEVERE, false, "(" + code + ")Failure On:" + sql_e);
                                    success = false;
                                    break;
                                }

                            }
                            sb = new StringBuilder();
                        }
                    }
                    //@TODO this will pause the main thread! Check if this is really needed
                    Thread.sleep(250);
                }
                stmt.close();
                sql_con.close();
            } catch (final Exception e) {
                WXTLogger.prettyLog(Level.SEVERE, false, "Failed to update db:" + e);
            } finally {
                try {
                    stmt.close();
                } catch (final SQLException e) {
                    WXTLogger.prettyLog(Level.FINE, false, e.getMessage());
                }
            }
            if (success) {
                WXTLogger.prettyLog(Level.INFO, false, "Successfully updated database.");
            } else {
                WXTLogger.prettyLog(Level.SEVERE, false, "Failed to update DB.");
            }
        } else {
            WXTLogger.prettyLog(Level.FINE, false, "Database is already up to date.");
        }
    }
}