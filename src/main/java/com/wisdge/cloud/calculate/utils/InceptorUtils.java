package com.wisdge.cloud.calculate.utils;

import lombok.extern.slf4j.Slf4j;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class InceptorUtils {
    //Hive2 Driver class name
    private static String driverName = "org.apache.hive.jdbc.HiveDriver";
    public static List<Map<String, Object>> getHistoryData(String acctId) throws SQLException {
        try {
            Class.forName(driverName);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        //Hive2 JDBC URL with LDAP
        String jdbcURL = "jdbc:hive2://10.5.113.115:10000/default";
        String user = "scrmuser";
        String password = "scrmuser@1234";
        Connection conn = DriverManager.getConnection(jdbcURL, user, password);
        log.info("inceptor连接成功");
        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery("select ta_code,acct_id,fund_code,agency_code,hold_share,frozen_share,confirm_date from dwh.hd_hold_arc " +
                "where confirm_date >= date_sub(to_date(sysdate), 365) and confirm_date < date_sub(to_date(sysdate), 1) and acct_id = '" + acctId + "'");
        log.info("inceptor执行sql成功");
        ResultSetMetaData rsmd = rs.getMetaData();
        int size = rsmd.getColumnCount();
        List<Map<String, Object>> list = new ArrayList<>();
        while(rs.next()) {
            Map<String, Object> rowData = new HashMap<>();
            for(int i = 0; i < size; i++) {
                rowData.put(rsmd.getColumnName(i+1), rs.getObject(i+1));
            }
            list.add(rowData);
        }
        log.info("inceptor执行结果：{}", list);
        rs.close();
        stmt.close();
        conn.close();
        return list;
    }
}
