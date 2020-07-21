package it.sotto.psd2.msa.massimali.util;


import com.google.common.primitives.Longs;
import it.sotto.psd2.msa.Config;
import it.sotto.psd2.msa.massimali.Constants;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Calendar;
import java.util.NavigableMap;

import it.sotto.psd2.msa.massimali.model.MassimaliModel;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.*;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.log4j.PropertyConfigurator;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.powermock.api.mockito.PowerMockito.*;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RunWith(PowerMockRunner.class)
@PrepareForTest({Config.class,CoverageUtils.class,Result.class})
public class TestHBaseAccess {

	private static final String LOG_PATH = "Massimali::HBaseAccess::";

	private byte[] fam_details = null;
	private byte[] col_max_daily = Bytes.toBytes("d-max");
	private byte[] col_max_monthly = Bytes.toBytes("m-max");
	private byte[] col_used_daily = Bytes.toBytes("d-used");
	private byte[] col_used_monthly = Bytes.toBytes("m-used");

	public TestHBaseAccess() {
		PropertyConfigurator.configure("log4j.properties");
		mockStatic(Config.class);

		when(Config.getValue(Constants.ENV_MASSIMALI_TABLENAME)).thenReturn("massimatiTable");
		when(Config.getValue(Constants.ENV_MASSIMALI_CF_DETAILS)).thenReturn("details");
		
		fam_details = Bytes.toBytes(Config.getValue(Constants.ENV_MASSIMALI_CF_DETAILS));
	}
	
	@Test
	public void test_createPut() {
		String rowkey = "00000|121212|RETAIL";
		BigDecimal maxDaily = new BigDecimal("250");
		BigDecimal maxMonthly = new BigDecimal("1500");
		BigDecimal usedDaily = new BigDecimal("100");
		BigDecimal usedMonthly = new BigDecimal("200");
		
		HBaseAccess db = new HBaseAccess();

		Put put1 = db.createPut(rowkey, maxDaily, maxMonthly);
				
		assertEquals(rowkey, Bytes.toString(put1.getRow()));
		assertEquals(maxDaily, Bytes.toBigDecimal(put1.get(fam_details, col_max_daily).get(0).getValue()));
		assertEquals(maxMonthly, Bytes.toBigDecimal(put1.get(fam_details, col_max_monthly).get(0).getValue()));
		
		Put put2 = db.createPut(rowkey, maxDaily, maxMonthly, false, usedDaily, usedMonthly);
				
		assertEquals(rowkey, Bytes.toString(put2.getRow()));
		assertEquals(maxDaily, Bytes.toBigDecimal(put2.get(fam_details, col_max_daily).get(0).getValue()));
		assertEquals(maxMonthly, Bytes.toBigDecimal(put2.get(fam_details, col_max_monthly).get(0).getValue()));
		assertEquals(usedDaily, Bytes.toBigDecimal(put2.get(fam_details, col_used_daily).get(0).getValue()));
		assertEquals(usedMonthly, Bytes.toBigDecimal(put2.get(fam_details, col_used_monthly).get(0).getValue()));
	}

	@Test
	public void testGetLogPath(){
		HBaseAccess db = new HBaseAccess();
		String logPath = db.getLogPath();

		assertEquals(logPath,LOG_PATH);
	}

	@Test
	public void testWarmUpConnectionCache() throws IOException {
		TableName massimaliTablename = TableName.valueOf(Config.getValue(Constants.ENV_MASSIMALI_TABLENAME));
		HBaseAccess db = new HBaseAccess();
		Connection connection = mock(Connection.class);
		RegionLocator regionLocator = mock(RegionLocator.class);
		when(connection.getRegionLocator(massimaliTablename)).thenReturn(regionLocator);
		db.warmUpConnectionCache("uuid",connection);
	}

	@Test(expected = Exception.class)
	public void testWarmUpConnectionCache_Exception() throws IOException {
		TableName massimaliTablename = TableName.valueOf(Config.getValue(Constants.ENV_MASSIMALI_TABLENAME));
		HBaseAccess db = new HBaseAccess();
		Connection connection = mock(Connection.class);
		when(connection.getRegionLocator(massimaliTablename)).thenReturn(null);
		db.warmUpConnectionCache("uuid",connection);
	}

	@Test
	public void testInquiry() throws IOException {
		BigDecimal amount = new BigDecimal(10);
		PowerMockito.mockStatic(CoverageUtils.class);
		PowerMockito.mock(Result.class);

		TableName massimaliTablename = TableName.valueOf(Config.getValue(Constants.ENV_MASSIMALI_TABLENAME));
		HBaseAccess db = mock(HBaseAccess.class);

		Connection connection = mock(Connection.class);
		RegionLocator regionLocator = mock(RegionLocator.class);
		Table table = mock(Table.class);
		Logger log = LoggerFactory.getLogger("apilog");

		when(connection.getRegionLocator(massimaliTablename)).thenReturn(regionLocator);
		when(connection.getTable(massimaliTablename)).thenReturn(table);

		when(db.inquiry("uuid", "00000", "uid", "RETAIL", "EUR", amount)).thenCallRealMethod();
		doCallRealMethod().when(db).getExternalConnection(connection);
		doCallRealMethod().when(db).getExternalLog(log);

		db.getExternalConnection(connection);
		db.getExternalLog(log);

		db.inquiry("uuid", "00000", "uid", "RETAIL", "EUR", amount);
	}

	@Test
	public void testGetMassimali() throws IOException {
		PowerMockito.mockStatic(CoverageUtils.class);

		TableName massimaliTablename = TableName.valueOf(Config.getValue(Constants.ENV_MASSIMALI_TABLENAME));
		HBaseAccess db = mock(HBaseAccess.class);

		Connection connection = mock(Connection.class);
		RegionLocator regionLocator = mock(RegionLocator.class);
		Table table = mock(Table.class);
		Logger log = LoggerFactory.getLogger("apilog");

		when(connection.getRegionLocator(massimaliTablename)).thenReturn(regionLocator);
		when(connection.getTable(massimaliTablename)).thenReturn(table);

		when(db.getMassimali("uuid","00000","uid","RETAIL")).thenCallRealMethod();
		doCallRealMethod().when(db).getExternalConnection(connection);
		doCallRealMethod().when(db).getExternalLog(log);

		db.getExternalConnection(connection);
		db.getExternalLog(log);

		db.getMassimali("uuid","00000","uid","RETAIL");
	}

	@Test (expected = Exception.class)
	public void testInquiry_Exeption() throws IOException {
		TableName massimaliTablename = TableName.valueOf(Config.getValue(Constants.ENV_MASSIMALI_TABLENAME));
		HBaseAccess db = new HBaseAccess();

		Connection connection = mock(Connection.class);
		RegionLocator regionLocator = mock(RegionLocator.class);
		Table table = mock(Table.class);

		when(connection.getRegionLocator(massimaliTablename)).thenReturn(regionLocator);
		when(connection.getTable(massimaliTablename)).thenThrow(IOException.class);

		db.getExternalConnection(connection);

		BigDecimal amount = new BigDecimal(10);
		db.inquiry("uuid", "0000", "uid", "usertype", "EUR", amount);
	}

	@Test(expected = Exception.class)
	public void testGetMassimali_Exception() throws IOException {
		TableName massimaliTablename = TableName.valueOf(Config.getValue(Constants.ENV_MASSIMALI_TABLENAME));
		HBaseAccess db = new HBaseAccess();

		Connection connection = mock(Connection.class);
		RegionLocator regionLocator = mock(RegionLocator.class);

		when(connection.getRegionLocator(massimaliTablename)).thenReturn(regionLocator);
		when(connection.getTable(massimaliTablename)).thenThrow(IOException.class);

		db.getExternalConnection(connection);

		db.getMassimali("uuid", "00000", "uid", "RETAIL");
	}

	@Test
	public void testUpdateMassimali() throws IOException {
		BigDecimal maxDaily = new BigDecimal(10);
		BigDecimal maxMounth = new BigDecimal(5);

		PowerMockito.mockStatic(CoverageUtils.class);

		TableName massimaliTablename = TableName.valueOf(Config.getValue(Constants.ENV_MASSIMALI_TABLENAME));
		HBaseAccess db = new HBaseAccess();

		Connection connection = mock(Connection.class);
		RegionLocator regionLocator = mock(RegionLocator.class);
		Table table = mock(Table.class);

		when(connection.getRegionLocator(massimaliTablename)).thenReturn(regionLocator);
		when(connection.getTable(massimaliTablename)).thenReturn(table);

		db.getExternalConnection(connection);

		db.updateMassimali("uuid","00000","uid","RETAIL",maxDaily,maxMounth);
	}

	@Test
	public void testUpdateMassimali_Exception() throws IOException {
		BigDecimal maxDaily = new BigDecimal(10);
		BigDecimal maxMounth = new BigDecimal(5);

		PowerMockito.mockStatic(CoverageUtils.class);

		TableName massimaliTablename = TableName.valueOf(Config.getValue(Constants.ENV_MASSIMALI_TABLENAME));
		HBaseAccess db = new HBaseAccess();

		Connection connection = mock(Connection.class);
		RegionLocator regionLocator = mock(RegionLocator.class);
		Table table = mock(Table.class);

		when(connection.getRegionLocator(massimaliTablename)).thenReturn(regionLocator);
		when(connection.getTable(massimaliTablename)).thenThrow(IOException.class);

		db.getExternalConnection(connection);

		assertEquals(db.updateMassimali("uuid","00000","uid","RETAIL",maxDaily,maxMounth),false);
	}

	@Test (expected = Exception.class)
	public void testInquiry_Exeption2() throws IOException {
		TableName massimaliTablename = TableName.valueOf(Config.getValue(Constants.ENV_MASSIMALI_TABLENAME));
		HBaseAccess db = new HBaseAccess();

		Connection connection = mock(Connection.class);
		RegionLocator regionLocator = mock(RegionLocator.class);
		Table table = mock(Table.class);

		when(connection.getRegionLocator(massimaliTablename)).thenReturn(regionLocator);
		when(connection.getTable(massimaliTablename)).thenReturn(table);

		db.getExternalConnection(connection);

		BigDecimal amount = new BigDecimal(10);
		db.inquiry("uuid", "0000", "uid", "usertype", "EUR", amount);
	}

	@Test(expected = Exception.class)
	public void testGet_resultMaggiorediZero_ServiceExecutionExceptionOld() throws IOException {
		PowerMockito.mockStatic(CoverageUtils.class);

		TableName massimaliTablename = TableName.valueOf(Config.getValue(Constants.ENV_MASSIMALI_TABLENAME));
		HBaseAccess db = new HBaseAccess();

		Connection connection = mock(Connection.class);
		RegionLocator regionLocator = mock(RegionLocator.class);
		Table table = mock(Table.class);
		Result result = mock(Result.class);

		when(table.get(any(Get.class))).thenReturn(result);
		when(result.size()).thenReturn(new Integer(1));
		when(connection.getRegionLocator(massimaliTablename)).thenReturn(regionLocator);
		when(connection.getTable(massimaliTablename)).thenReturn(table);

		db.getExternalConnection(connection);
		db.get("uuid", table, "TEST", false);
	}

	@Test(expected = Exception.class)
	public void testGet_resultMaggiorediZero_ServiceExecutionExceptionOld2() throws IOException {
		PowerMockito.mockStatic(CoverageUtils.class);

		TableName massimaliTablename = TableName.valueOf(Config.getValue(Constants.ENV_MASSIMALI_TABLENAME));
		HBaseAccess db = new HBaseAccess();

		Connection connection = mock(Connection.class);
		RegionLocator regionLocator = mock(RegionLocator.class);
		Table table = mock(Table.class);
		Result result = mock(Result.class);
		NavigableMap<byte[], byte[]> detailsMap = mock(NavigableMap.class);

		when(table.get(any(Get.class))).thenReturn(result);

		when(result.size()).thenReturn(new Integer(1));
		when(result.getFamilyMap(fam_details)).thenReturn(detailsMap);

		when(connection.getRegionLocator(massimaliTablename)).thenReturn(regionLocator);
		when(connection.getTable(massimaliTablename)).thenReturn(table);

		db.getExternalConnection(connection);
		db.get("uuid", table, "TEST", false);
	}

	@Test
	public void testGet_resultMaggiorediZero() throws IOException {
		PowerMockito.mockStatic(CoverageUtils.class);

		TableName massimaliTablename = TableName.valueOf(Config.getValue(Constants.ENV_MASSIMALI_TABLENAME));
		HBaseAccess db = new HBaseAccess();

		Connection connection = mock(Connection.class);
		RegionLocator regionLocator = mock(RegionLocator.class);
		Table table = mock(Table.class);
		Result result = mock(Result.class);
		NavigableMap<byte[], byte[]> detailsMap = mock(NavigableMap.class);
		byte[] bytes = new byte[] {12};

		when(table.get(any(Get.class))).thenReturn(result);

		when(result.size()).thenReturn(new Integer(1));
		when(result.getFamilyMap(fam_details)).thenReturn(detailsMap);

		when(detailsMap.get(col_max_daily)).thenReturn(bytes);
		when(detailsMap.get(col_max_monthly)).thenReturn(bytes);
		when(detailsMap.get(col_used_daily)).thenReturn(bytes);
		when(detailsMap.get(col_used_monthly)).thenReturn(bytes);

		when(connection.getRegionLocator(massimaliTablename)).thenReturn(regionLocator);
		when(connection.getTable(massimaliTablename)).thenReturn(table);

		db.getExternalConnection(connection);
		db.get("uuid", table, "TEST", false);
		db.get("uuid", table, "TEST", true);
	}

	@Test(expected = Exception.class)
	public void testGet_resultUgualeAZero_ServiceExecutionExceptionOld() throws IOException {
		PowerMockito.mockStatic(CoverageUtils.class);

		TableName massimaliTablename = TableName.valueOf(Config.getValue(Constants.ENV_MASSIMALI_TABLENAME));
		HBaseAccess db = new HBaseAccess();

		Connection connection = mock(Connection.class);
		RegionLocator regionLocator = mock(RegionLocator.class);
		Table table = mock(Table.class);
		Result result = mock(Result.class);

		when(Config.getValue(Constants.ENV_DEFAULT_MAX_DAILY)).thenReturn("10");
		when(Config.getValue(Constants.ENV_DEFAULT_MAX_MONTHLY)).thenReturn("20");

		when(table.get(any(Get.class))).thenReturn(result);
		when(result.size()).thenReturn(new Integer(0));
		when(connection.getRegionLocator(massimaliTablename)).thenReturn(regionLocator);
		when(connection.getTable(massimaliTablename)).thenReturn(table);

		db.getExternalConnection(connection);
		db.get("uuid", table, "TEST", false);
	}


	@Test
	public void testGet_resultUgualeAZero() throws IOException {
		MassimaliModel model = new MassimaliModel();

		PowerMockito.mockStatic(CoverageUtils.class);

		TableName massimaliTablename = TableName.valueOf(Config.getValue(Constants.ENV_MASSIMALI_TABLENAME));
		HBaseAccess db = new HBaseAccess();

		Connection connection = mock(Connection.class);
		RegionLocator regionLocator = mock(RegionLocator.class);
		Table table = mock(Table.class);
		Result result = mock(Result.class);

		when(Config.getValue(Constants.ENV_DEFAULT_MAX_DAILY)).thenReturn("10");
		when(Config.getValue(Constants.ENV_DEFAULT_MAX_MONTHLY)).thenReturn("110");

		when(table.get(any(Get.class))).thenReturn(result);
		when(table.checkAndPut(any(byte[].class),any(byte[].class),any(byte[].class),isNull(byte[].class),any(Put.class))).thenReturn(true);

		when(result.size()).thenReturn(new Integer(0));
		when(connection.getRegionLocator(massimaliTablename)).thenReturn(regionLocator);
		when(connection.getTable(massimaliTablename)).thenReturn(table);

		db.getExternalConnection(connection);
		db.get("uuid", table, "TEST", true);
	}


	@Test
	public void testUpdate_KO() throws IOException {
		final byte[] col_timestamp = Bytes.toBytes("tms");
		byte[] timestamp = Longs.toByteArray(1);

		TableName massimaliTablename = TableName.valueOf(Config.getValue(Constants.ENV_MASSIMALI_TABLENAME));
		HBaseAccess db = new HBaseAccess();

		Connection connection = mock(Connection.class);
		RegionLocator regionLocator = mock(RegionLocator.class);
		Table table = mock(Table.class);
		Result result = mock(Result.class);

		when(Config.getValue(Constants.ENV_DEFAULT_MAX_DAILY)).thenReturn("10");
		when(Config.getValue(Constants.ENV_DEFAULT_MAX_MONTHLY)).thenReturn("110");

		when(table.get(any(Get.class))).thenReturn(result);
		when(table.checkAndPut(any(byte[].class),any(byte[].class),any(byte[].class),isNull(byte[].class),any(Put.class))).thenReturn(true);

		when(result.size()).thenReturn(new Integer(0));
		when(result.getValue(fam_details, col_timestamp)).thenReturn(timestamp);
		when(connection.getRegionLocator(massimaliTablename)).thenReturn(regionLocator);
		when(connection.getTable(massimaliTablename)).thenReturn(table);

		db.getExternalConnection(connection);

		assertEquals(db.update("uuid", "00000", "uid", "RETAIL", "EUR", new BigDecimal(10)),"KO");
	}

	@Test
	public void testUpdate_IOException() throws IOException {
		TableName massimaliTablename = TableName.valueOf(Config.getValue(Constants.ENV_MASSIMALI_TABLENAME));
		HBaseAccess db = new HBaseAccess();

		Connection connection = mock(Connection.class);
		RegionLocator regionLocator = mock(RegionLocator.class);

		when(connection.getRegionLocator(massimaliTablename)).thenReturn(regionLocator);
		when(connection.getTable(massimaliTablename)).thenThrow(IOException.class);

		db.getExternalConnection(connection);

		assertEquals(db.update("uuid", "00000", "uid", "RETAIL", "EUR", new BigDecimal(10)),"KO");
	}

	@Test
	public void testUpdate() throws IOException {
		final byte[] col_timestamp = Bytes.toBytes("tms");
		byte[] timestamp = Longs.toByteArray(1);

		TableName massimaliTablename = TableName.valueOf(Config.getValue(Constants.ENV_MASSIMALI_TABLENAME));
		HBaseAccess db = new HBaseAccess();

		Connection connection = mock(Connection.class);
		RegionLocator regionLocator = mock(RegionLocator.class);
		Table table = mock(Table.class);
		Result result = mock(Result.class);

		when(Config.getValue(Constants.ENV_DEFAULT_MAX_DAILY)).thenReturn("10");
		when(Config.getValue(Constants.ENV_DEFAULT_MAX_MONTHLY)).thenReturn("110");

		when(table.get(any(Get.class))).thenReturn(result);
		when(table.checkAndPut(any(byte[].class),any(byte[].class),any(byte[].class),isNull(byte[].class),any(Put.class))).thenReturn(true);
		when(table.checkAndPut(any(byte[].class),any(byte[].class),any(byte[].class),any(byte[].class),any(Put.class))).thenReturn(true);

		when(result.size()).thenReturn(new Integer(0));
		when(result.getValue(fam_details, col_timestamp)).thenReturn(timestamp);
		when(connection.getRegionLocator(massimaliTablename)).thenReturn(regionLocator);
		when(connection.getTable(massimaliTablename)).thenReturn(table);

		db.getExternalConnection(connection);

		assertEquals(db.update("uuid", "00000", "uid", "RETAIL", "EUR", new BigDecimal(10)),"OK");
	}

	@Test
	public void testUpdate_Current_Date() throws IOException {
		final byte[] col_timestamp = Bytes.toBytes("tms");
		Calendar calendar = Calendar.getInstance();
		Long aLong = calendar.getTimeInMillis();
		byte[] timestamp = Longs.toByteArray(aLong);

		TableName massimaliTablename = TableName.valueOf(Config.getValue(Constants.ENV_MASSIMALI_TABLENAME));
		HBaseAccess db = new HBaseAccess();

		Connection connection = mock(Connection.class);
		RegionLocator regionLocator = mock(RegionLocator.class);
		Table table = mock(Table.class);
		Result result = mock(Result.class);

		when(Config.getValue(Constants.ENV_DEFAULT_MAX_DAILY)).thenReturn("10");
		when(Config.getValue(Constants.ENV_DEFAULT_MAX_MONTHLY)).thenReturn("110");

		when(table.get(any(Get.class))).thenReturn(result);
		when(table.checkAndPut(any(byte[].class),any(byte[].class),any(byte[].class),isNull(byte[].class),any(Put.class))).thenReturn(true);
		when(table.checkAndPut(any(byte[].class),any(byte[].class),any(byte[].class),any(byte[].class),any(Put.class))).thenReturn(true);

		when(result.size()).thenReturn(new Integer(0));
		when(result.getValue(fam_details, col_timestamp)).thenReturn(timestamp);
		when(connection.getRegionLocator(massimaliTablename)).thenReturn(regionLocator);
		when(connection.getTable(massimaliTablename)).thenReturn(table);

		db.getExternalConnection(connection);

		assertEquals(db.update("uuid", "00000", "uid", "RETAIL", "EUR", new BigDecimal(10)),"OK");
	}

	@Test
	public void testUpdate_ResponseFailure() throws IOException {
		final byte[] col_timestamp = Bytes.toBytes("tms");
		Calendar calendar = Calendar.getInstance();
		Long aLong = calendar.getTimeInMillis();
		byte[] timestamp = Longs.toByteArray(aLong);

		PowerMockito.mockStatic(CoverageUtils.class);
		when(CoverageUtils.isPaymentCovered(any(MassimaliModel.class),any(BigDecimal.class))).thenReturn("KO");

		TableName massimaliTablename = TableName.valueOf(Config.getValue(Constants.ENV_MASSIMALI_TABLENAME));
		HBaseAccess db = new HBaseAccess();

		Connection connection = mock(Connection.class);
		RegionLocator regionLocator = mock(RegionLocator.class);
		Table table = mock(Table.class);
		Result result = mock(Result.class);

		when(Config.getValue(Constants.ENV_DEFAULT_MAX_DAILY)).thenReturn("10");
		when(Config.getValue(Constants.ENV_DEFAULT_MAX_MONTHLY)).thenReturn("110");

		when(table.get(any(Get.class))).thenReturn(result);
		when(table.checkAndPut(any(byte[].class),any(byte[].class),any(byte[].class),isNull(byte[].class),any(Put.class))).thenReturn(true);
		when(table.checkAndPut(any(byte[].class),any(byte[].class),any(byte[].class),any(byte[].class),any(Put.class))).thenReturn(true);

		when(result.size()).thenReturn(new Integer(0));
		when(result.getValue(fam_details, col_timestamp)).thenReturn(timestamp);
		when(connection.getRegionLocator(massimaliTablename)).thenReturn(regionLocator);
		when(connection.getTable(massimaliTablename)).thenReturn(table);

		db.getExternalConnection(connection);

		assertEquals(db.update("uuid", "00000", "uid", "RETAIL", "EUR", new BigDecimal(10)),"KO");
	}
}