package it.sotto.psd2.msa.massimali.util;

import static akka.http.javadsl.model.StatusCodes.NOT_FOUND;
import it.sotto.psd2.msa.exception.ServiceExecutionExceptionOld;
import it.sotto.psd2.msa.exception.model.ErrorCodeOld;
import static it.sotto.psd2.msa.massimali.Constants.RESPONSE_FAILURE;
import static it.sotto.psd2.msa.massimali.Constants.RESPONSE_SUCCESS;
import it.sotto.psd2.msa.massimali.model.MassimaliModel;
import java.math.BigDecimal;
import static java.math.BigDecimal.ZERO;
import java.util.Optional;
import static java.util.Optional.ofNullable;

/**
 *
 * @author Oian Andrea
 */
public class CoverageUtils {
	
	public static String isPaymentCovered(MassimaliModel model, BigDecimal amount) {
		Optional<MassimaliModel> om = ofNullable(model);
		if(
			om.isPresent() && 
			om.map(MassimaliModel::getUtilizzatoGiornalieroOdierno).map(v -> v.compareTo(ZERO)>=0).orElse(false) &&
			om.map(MassimaliModel::getMaxGiornaliero).map(v -> v.compareTo(ZERO)>=0).orElse(false) &&
			om.map(MassimaliModel::getUtilizzatoMensileCorrente).map(v -> v.compareTo(ZERO)>=0).orElse(false) &&
			om.map(MassimaliModel::getMaxMensile).map(v -> v.compareTo(ZERO)>=0).orElse(false)
		)
			return (
				(model.getUtilizzatoGiornalieroOdierno().add(amount).compareTo(model.getMaxGiornaliero())<=0)
				&& 
				(model.getUtilizzatoMensileCorrente().add(amount).compareTo(model.getMaxMensile())<=0)
			)
				?RESPONSE_SUCCESS
				:RESPONSE_FAILURE;
		throw new ServiceExecutionExceptionOld(NOT_FOUND, ErrorCodeOld.DATA_NOT_FOUND, "account data not found");
	}
}
