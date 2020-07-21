package it.sotto.psd2.msa.massimali.util;

import it.sotto.psd2.msa.massimali.model.MassimaliModel;
import java.math.BigDecimal;

/**
 * 
 * @author Oian Andrea
 *
 */
public interface MassimaliDataAccess {
	public String inquiry(String uuid, String abi, String uid, String usertype, String currency, BigDecimal amount);
	public String update(String uuid, String abi, String uid, String usertype, String currency, BigDecimal amount);
	
	public MassimaliModel getMassimali(String uuid, String abi, String uid, String usertype);
	public boolean updateMassimali(String uuid, String abi, String uid, String usertype, BigDecimal maxDaily, BigDecimal maxMonthly);
	
	public boolean healthcheck();
}