package benchmark;

import java.io.*;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 *
 * @author njogun
 */
public class Benchmark {

    final static String HOST = "dbhost",
            PORT = "1521",
            SID = "dbname",
            USERNAME = "dbuser",
            PASSWORD = "dbpass";
    final static String INPUT = "input.txt";
    
    private final static String BASEDIR = "c:/users/njogun/desktop/benchmark/";
    static String INPUTFILE = BASEDIR + INPUT + ".csv";
    static String SORTEDFILE = BASEDIR + INPUT + "_sorted.csv";
    static String PROPSFILE = BASEDIR + INPUT + ".properties";    
    static String LOGFILE = BASEDIR + INPUT + ".log";
    
    static Properties props = new Properties();
    static Connection db = null;
    static BufferedWriter logger = null;

    public static void OpenConnection() {
        try {
            Class.forName("oracle.jdbc.driver.OracleDriver");
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }

        try {
            String uri = "jdbc:oracle:thin:@" + HOST + ":" + PORT + ":" + SID;
            db = DriverManager.getConnection(uri, USERNAME, PASSWORD);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(2);
        }
    }

    public static void CloseConnection() {
        try {
            db.close();
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(3);
        }
    }

    public static String LoadTime() {
        try {
            props.load(new FileInputStream(PROPSFILE));
        } catch (FileNotFoundException ex) {
            //System.out.println("First load, file doesn't exist yet.");
        } catch (IOException ex) {
            ex.printStackTrace();
            System.exit(4);
        }
        String time = props.getProperty("time");
        if (time == null) {
            return "08:00:00";
        } else {
            return time;
        }
    }

    public static void SaveTime(String time) {
        props.setProperty("time", time);
        try {
            props.store(new FileOutputStream(PROPSFILE), null);
        } catch (IOException ex) {
            ex.printStackTrace();
            System.exit(5);
        }
    }

    public static ArrayList<String> GetInput(String currentTimeslot) {
        //System.out.println("... looking for " + currentTimeslot);        
        logprint("... looking for " + currentTimeslot);
        BufferedReader br;
        ArrayList<String> list = new ArrayList<>();
        try {
            br = new BufferedReader(new FileReader(SORTEDFILE));
            String line;
            String[] arr;
            while ((line = br.readLine()) != null) {
                //System.out.println("... line: " + line);
                if (line.startsWith("Time")) {
                    continue;
                }
                arr = line.split(",");
                StringBuilder sb = new StringBuilder(arr[0]);
                sb.deleteCharAt(0);
                sb.deleteCharAt(sb.length() - 1);
                arr[0] = sb.toString();
                //System.out.println("... " + currentTimeslot + " vs. " + arr[0]);
                if (arr[0].compareTo(currentTimeslot) < 0) {
                    continue;
                } else if (arr[0].equals(currentTimeslot)) {
                    list.add(line);
                } else if (arr[0].compareTo(currentTimeslot) > 0) {
                    //System.out.println("... saving " + arr[0]);
                    logprint("... saving " + arr[0]);
                    SaveTime(arr[0]);
                    return list;
                }
            }
            br.close();
            SaveTime("16:00:00");
        } catch (IOException ex) {
            ex.printStackTrace();
            System.exit(6);
        }
        return list;
    }

    public static int SendSql(String sql) throws SQLException {
        ResultSet rs;
        Statement stmt;
        stmt = db.createStatement();
        rs = stmt.executeQuery(sql);
        rs.next();
        int retval = rs.getInt(1);
        rs.close();
        //System.out.println("... result: " + rs.getInt(1));         
        return retval;
    }

    public static void logprint(String msg) {
        try {
            logger.write(msg);
            logger.newLine();
            logger.flush();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        ArrayList<String> list = new ArrayList<>();

        // sort input file if not sorted
        BufferedReader br = null;
        BufferedWriter bw = null;        
        if (!(new File(SORTEDFILE)).exists()) {
            try {
                br = new BufferedReader(new FileReader(INPUTFILE));
                String line;
                while ((line = br.readLine()) != null) {
                    list.add(line);
                }
                br.close();
                Collections.sort(list);
                bw = new BufferedWriter(new FileWriter(SORTEDFILE));
                for (String s : list) {
                    bw.write(s);
                    bw.newLine();
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            } finally {
                try {
                    br.close();
                    bw.close();
                } catch (IOException ex) {
                    ex.printStackTrace();
                    System.exit(11);
                }
            }
            System.out.println("Done sorting file.");
            //System.exit(0);            
        }

        try {
            logger = new BufferedWriter(new FileWriter(LOGFILE));
        } catch (IOException ex) {
            ex.printStackTrace();
        }


        Calendar cal = Calendar.getInstance();
        SimpleDateFormat df = new SimpleDateFormat();
        df.applyPattern("YYYYMMdd");
        //System.out.println("... today: " + df.format(cal.getTime()));        
        logprint("... today: " + df.format(cal.getTime()));
        String DATOBR1, DATOBR2;
        cal.add(Calendar.DAY_OF_YEAR, -365);
        DATOBR1 = df.format(cal.getTime());
        cal.add(Calendar.DAY_OF_YEAR, 365 - 21);
        DATOBR2 = df.format(cal.getTime());

        String timeslot;
        int counter = 0;
        while ((timeslot = LoadTime()).compareTo("16:00:00") < 0) {
            logprint("Run #" + (++counter));
            System.out.print("Working on " + timeslot);
            OpenConnection();
            list = GetInput(timeslot);
            System.out.println(" (" + list.size() + " inputs)...");
            String[] arr;
            String KORINS, KORPAR;
            String sql;
            long before, after;
            int result = -1;
            for (String s : list) {
                arr = s.split(",");
                KORINS = arr[1];
                KORPAR = arr[2];
                sql = "here goes sql statement with variables KORINS, KORPAR, DATOBR1 and DATOBR2";
                before = System.currentTimeMillis();
                //System.out.println(sql);
                try {
                    result = SendSql(sql);
                } catch (SQLException ex) {
                    System.err.println("SQLException: " + sql);
                    ex.printStackTrace();
                }
                after = System.currentTimeMillis();
                logprint(sql);
                logprint("Result = " + result);
                //System.out.println("Time taken: " + (after - before)/1000.0);
                logprint("Time taken: " + (after - before) / 1000.0);
                //break;
            };
            CloseConnection();
        }

        System.out.println("Done.");

        try {
            logger.close();
        } catch (IOException ex) {
            ex.printStackTrace();
            System.exit(13);
        }
    }
}
