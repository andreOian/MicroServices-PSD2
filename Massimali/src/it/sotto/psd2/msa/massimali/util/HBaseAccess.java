package it.sotto.psd2.msa.massimali.util;

import com.fasterxml.jackson.annotation.JacksonInject;
import it.sotto.psd2.msa.Config;

import static it.sotto.psd2.msa.Config.getValue;

import it.sotto.psd2.msa.db.AbstractHBaseAccess;
import it.sotto.psd2.msa.exception.ServiceExecutionExceptionOld;
import it.sotto.psd2.msa.massimali.Constants;

import static it.sotto.psd2.msa.massimali.Constants.RESPONSE_FAILURE;
import static it.sotto.psd2.msa.massimali.Constants.RESPONSE_SUCCESS;

import it.sotto.psd2.msa.massimali.model.MassimaliModel;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Date;
import java.util.NavigableMap;

import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.RegionLocator;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.Table;
import org.apache.hadoop.hbase.util.Bytes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Oian Andrea
 */
public class HBaseAccess extends AbstractHBaseAccess implements MassimaliDataAccess {
    private static final String LOG_PATH = "Massimali::HBaseAccess::";

    private final TableName massimaliTablename = TableName.valueOf(Config.getValue(Constants.ENV_MASSIMALI_TABLENAME));

    private static final byte[] fam_details = Bytes.toBytes(Config.getValue(Constants.ENV_MASSIMALI_CF_DETAILS));

    private static final byte[] col_timestamp = Bytes.toBytes("tms");
    private static final byte[] col_max_daily = Bytes.toBytes("d-max");
    private static final byte[] col_used_daily = Bytes.toBytes("d-used");
    private static final byte[] col_max_monthly = Bytes.toBytes("m-max");
    private static final byte[] col_used_monthly = Bytes.toBytes("m-used");

    private boolean accessError = false;

    @Override
    protected void warmUpConnectionCache(String uuid, Connection connection) throws IOException {
        try (RegionLocator locator = connection.getRegionLocator(massimaliTablename)) {
            baselog.info("Warmed up region location cache for {} got {}", massimaliTablename, locator.getAllRegionLocations().size());
        }
    }

    @Override
    protected String getLogPath() {
        return LOG_PATH;
    }

    private String getRowKey(String abi, String uid, String usertype) {
        return salt(abi + "|" + uid + "|" + usertype, Constants.HBASE_REGIONS);
    }

    @Override
    public String inquiry(String uuid, String abi, String uid, String usertype, String currency, BigDecimal amount) {
        log.debug(
                "inquiry abi[{}] uid[{}] usertype[{}] amount[{}]",
                abi, uid, usertype, amount
        );
        try (Table table = connection.getTable(massimaliTablename)) {
            String rowkey = getRowKey(abi, uid, usertype);
            log.info("inquiry {}", rowkey);
            String s = CoverageUtils.isPaymentCovered(get(uuid, table, rowkey, false), amount);
            return s;
        } catch (IOException e) {
            log.error("inquiry exception fetching record", e);
            throw new ServiceExecutionExceptionOld();
        }
    }

    /**
     * L'implementazione di questo metodo è sbagliata.
     * <p>
     * Bisognerebbe eseguire un'aoperazione atomica tipo (in sql):
     * update table set field=field+amount, timestamp=now where rowkey=?
     * ...ma apparentemente HBASE client non lo permette.
     *
     * @param uuid
     * @param abi
     * @param uid
     * @param usertype
     * @param currency
     * @param amount
     * @return
     */
    @Override
    public String update(String uuid, String abi, String uid, String usertype, String currency, BigDecimal amount) {
        log.debug(
                "update abi[{}] uid[{}] usertype[{}] amount[{}]",
                abi, uid, usertype, amount
        );
        try (Table table = connection.getTable(massimaliTablename)) {
            String rowkey = getRowKey(abi, uid, usertype);
            log.info("update {}", rowkey);

            byte[] rowId = Bytes.toBytes(rowkey);

            MassimaliModel model = get(uuid, table, rowkey, false);

            String s = CoverageUtils.isPaymentCovered(model, amount);
            if (CoverageUtils.isPaymentCovered(model, amount).equals(RESPONSE_FAILURE)) {
                log.error("error updating rowkey [{}] > maximum coverage exceeded", rowkey);
                return RESPONSE_FAILURE;
            }

            Get get = new Get(rowId);
            get.addFamily(fam_details);
            Result result = table.get(get);

//			NavigableMap<byte[], byte[]> detailsMap = result.getFamilyMap(fam_details);

            Calendar lastUpdate = Calendar.getInstance();
            byte[] timestamp = result.getValue(fam_details, col_timestamp);
            lastUpdate.setTime(new Date(Bytes.toLong(timestamp)));

            Calendar currentDate = Calendar.getInstance();
            currentDate.setTime(new Date());

            Put p = new Put(rowId);
            p.addColumn(fam_details, col_used_daily, Bytes.toBytes(
                    (
                            (lastUpdate.get(Calendar.YEAR) == currentDate.get(Calendar.YEAR)) &&
                                    (lastUpdate.get(Calendar.MONTH) == currentDate.get(Calendar.MONTH)) &&
                                    (lastUpdate.get(Calendar.DATE) == currentDate.get(Calendar.DATE))
                    )
                            ? model.getUtilizzatoGiornalieroOdierno().add(amount)
                            : amount
            ));
            p.addColumn(fam_details, col_used_monthly, Bytes.toBytes(
                    (
                            (lastUpdate.get(Calendar.YEAR) == currentDate.get(Calendar.YEAR)) &&
                                    (lastUpdate.get(Calendar.MONTH) == currentDate.get(Calendar.MONTH))
                    )
                            ? model.getUtilizzatoMensileCorrente().add(amount)
                            : amount
            ));

            boolean b = !table.checkAndPut(rowId, fam_details, col_timestamp, timestamp, p);
            if (!table.checkAndPut(rowId, fam_details, col_timestamp, timestamp, p)) {
                log.error("error updating rowkey [{}] > invalid TMS [{}]", rowkey, lastUpdate.getTimeInMillis());
                return RESPONSE_FAILURE;
            }
        } catch (IOException e) {
            log.error("update - exception updating record", e);
            return RESPONSE_FAILURE;
//			throw new ServiceExecutionException();
        }
        return RESPONSE_SUCCESS;
    }

    @Override
    public MassimaliModel getMassimali(String uuid, String abi, String uid, String usertype) {
        log.debug(
                "getMassimali abi [{}] uid [{}] usertype [{}]",
                abi, uid, usertype
        );
        try (Table table = connection.getTable(massimaliTablename)) {
            String rowkey = getRowKey(abi, uid, usertype);
            log.info("getMassimali {}", rowkey);
            return get(uuid, table, rowkey, true);
        } catch (IOException e) {
            log.error("getMassimali exception fetching record", e);
            throw new ServiceExecutionExceptionOld();
        }
    }

    @Override
    public boolean updateMassimali(String uuid, String abi, String uid, String usertype, BigDecimal maxDaily, BigDecimal maxMonthly) {
        log.debug(
                "updateMassimali abi[{}] uid[{}] usertype[{}] maxDaily[{}] maxMonthly[{}]",
                abi, uid, usertype,
                ((maxDaily == null) ? null : maxDaily.toPlainString()),
                ((maxMonthly == null) ? null : maxMonthly.toPlainString())
        );
        try (Table table = connection.getTable(massimaliTablename)) {
            String rowkey = getRowKey(abi, uid, usertype);
            log.info("updateMassimali {}", rowkey);
            table.put(createPut(rowkey, maxDaily, maxMonthly));
        } catch (IOException e) {
            log.error("updateMassimali exception updating record", e);
            return false;
        }
        return true;
    }

    protected Put createPut(String rowkey, BigDecimal maxDaily, BigDecimal maxMonthly) {
        Put p = new Put(Bytes.toBytes(rowkey));
        p.addColumn(fam_details, col_max_daily, Bytes.toBytes(maxDaily));
        p.addColumn(fam_details, col_max_monthly, Bytes.toBytes(maxMonthly));
        return p;
    }

    protected Put createPut(
            String rowkey,
            BigDecimal maxDaily, BigDecimal maxMonthly,
            boolean bmedAPI,
            BigDecimal usedDaily, BigDecimal usedMonthly
    ) {
        Put p = createPut(rowkey, maxDaily, maxMonthly);
        if (!bmedAPI) {
            p.addColumn(fam_details, col_used_daily, Bytes.toBytes(usedDaily));
            p.addColumn(fam_details, col_used_monthly, Bytes.toBytes(usedMonthly));
        }
        return p;
    }

    protected MassimaliModel get(String uuid, Table table, String rowkey, boolean bmedAPI) throws IOException {
        byte[] rowId = Bytes.toBytes(rowkey);
        Get get = new Get(rowId);
        Result result = table.get(get);
        MassimaliModel model = new MassimaliModel();

        if (result.size() == 0) {
            log.debug("no rows found for rowkey {}", rowkey);

            model.setMaxGiornaliero(new BigDecimal(getValue(Constants.ENV_DEFAULT_MAX_DAILY)));
            model.setMaxMensile(new BigDecimal(getValue(Constants.ENV_DEFAULT_MAX_MONTHLY)));
            model.setUtilizzatoGiornalieroOdierno(BigDecimal.ZERO);
            model.setUtilizzatoMensileCorrente(BigDecimal.ZERO);

            log.debug("inserting data for rowkey {}", rowkey);

            Put p = createPut(
                    rowkey, model.getMaxGiornaliero(), model.getMaxMensile(),
                    bmedAPI, model.getUtilizzatoGiornalieroOdierno(), model.getUtilizzatoMensileCorrente()
            );

            if (!table.checkAndPut(rowId, fam_details, col_timestamp, null, p)) {
                log.error("HBaseService::get - error creating rowkey {}, record already exists!", rowkey);
                throw new ServiceExecutionExceptionOld();
            } else
                log.info("HBaseService::put executed - rowkey {}", rowkey);
        } else {
            NavigableMap<byte[], byte[]> detailsMap = result.getFamilyMap(fam_details);
            if (detailsMap == null) {
                log.error("HBaseService::get - inconsistent data for rowkey {}", rowkey);
                throw new ServiceExecutionExceptionOld();
            }
            try {
                model.setMaxGiornaliero(Bytes.toBigDecimal(detailsMap.get(col_max_daily)));
                model.setMaxMensile(Bytes.toBigDecimal(detailsMap.get(col_max_monthly)));
                if (!bmedAPI) {
                    model.setUtilizzatoGiornalieroOdierno(Bytes.toBigDecimal(detailsMap.get(col_used_daily)));
                    model.setUtilizzatoMensileCorrente(Bytes.toBigDecimal(detailsMap.get(col_used_monthly)));
                }
            } catch (Exception e) {
                accessError = true;
                log.error("HBaseService::get values - inconsistent data", e);
                throw new ServiceExecutionExceptionOld();
            }
        }
        return model;
    }

    @Override
    public boolean healthcheck() {
        boolean ok = super.healthcheck();
        if (!ok)
            return ok;
        return !accessError;
    }

    protected void getExternalConnection(Connection connection) {
        this.connection = connection;
    }

    protected void getExternalLog (Logger logger) {
        this.log = logger;
    }

}
