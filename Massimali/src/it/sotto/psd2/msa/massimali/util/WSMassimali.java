package it.sotto.psd2.msa.massimali.util;

import it.sotto.psd2.msa.model.AccountsModel;
import java.math.BigDecimal;

/**
 * 
 * @author Oian Andrea
 *
 */
public interface WSMassimali {
	public String inquiry(String uuid, String abi, String userType, String lang, String currency, BigDecimal amount, AccountsModel accounts);
	public String update(String uuid, String abi, String userType, String lang, String currency, BigDecimal amount, AccountsModel accounts);
	public boolean healthcheck();
}