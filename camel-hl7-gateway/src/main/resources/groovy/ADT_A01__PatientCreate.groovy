import ca.uhn.hl7v2.model.Message;
import ca.uhn.hl7v2.model.v24.segment.PID;
import org.hl7.fhir.dstu3.model.HumanName;
import org.hl7.fhir.dstu3.model.Patient;

hl7v2 = request.body
hl7v2Pid = hl7v2.get('PID');

patient = new Patient();
hl7v2Pid.getPatientName().groupBy({ patientName -> patientName.getFamilyName().getSurname().getValue() }).each() { familyName, patientNames ->
  patientName = new HumanName();
  patientNames.each() {
    patientName.addGiven(it.getGivenName().getValue());
  }
  patientName.setFamily(familyName);
  patient.addName(patientName);
}
patient.setId(hl7v2Pid.getPatientID().getID().getValue());

return patient;