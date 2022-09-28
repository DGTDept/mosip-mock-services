package io.mosip.mock.sdk.impl;

import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Base64.Encoder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import io.mosip.mock.sdk.utils.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.stereotype.Component;

import io.mosip.kernel.bio.converter.constant.ConverterErrorCode;
import io.mosip.kernel.bio.converter.exception.ConversionException;
import io.mosip.kernel.bio.converter.service.impl.ConverterServiceImpl;
import io.mosip.kernel.biometrics.constant.BiometricFunction;
import io.mosip.kernel.biometrics.constant.BiometricType;
import io.mosip.kernel.biometrics.constant.Match;
import io.mosip.kernel.biometrics.entities.BIR;
import io.mosip.kernel.biometrics.entities.BiometricRecord;
import io.mosip.kernel.biometrics.model.Decision;
import io.mosip.kernel.biometrics.model.MatchDecision;
import io.mosip.kernel.biometrics.model.QualityCheck;
import io.mosip.kernel.biometrics.model.QualityScore;
import io.mosip.kernel.biometrics.model.Response;
import io.mosip.kernel.biometrics.model.SDKInfo;
import io.mosip.kernel.biometrics.spi.IBioApi;
import io.mosip.mock.sdk.constant.ResponseStatus;

/**
 * The Class BioApiImpl.
 * 
 * @author Sanjay Murali
 * @author Manoj SP
 * 
 */
@Component
@EnableAutoConfiguration
public class SampleSDK implements IBioApi {

	Logger LOGGER = LoggerFactory.getLogger(SampleSDK.class);

	private static final String API_VERSION = "0.9";

	@Override
	public SDKInfo init(Map<String, String> initParams) {
		// TODO validate for mandatory initParams
		SDKInfo sdkInfo = new SDKInfo(API_VERSION, "sample", "sample", "sample");
		List<BiometricType> supportedModalities = new ArrayList<>();
		supportedModalities.add(BiometricType.FINGER);
		supportedModalities.add(BiometricType.FACE);
		supportedModalities.add(BiometricType.IRIS);
		sdkInfo.setSupportedModalities(supportedModalities);
		Map<BiometricFunction, List<BiometricType>> supportedMethods = new HashMap<>();
		supportedMethods.put(BiometricFunction.MATCH, supportedModalities);
		supportedMethods.put(BiometricFunction.QUALITY_CHECK, supportedModalities);
		supportedMethods.put(BiometricFunction.EXTRACT, supportedModalities);
		sdkInfo.setSupportedMethods(supportedMethods);
		return sdkInfo;
	}

	@Override
	public Response<QualityCheck> checkQuality(BiometricRecord sample, List<BiometricType> modalitiesToCheck,
			Map<String, String> flags) {
		Response<QualityCheck> response = new Response<>();
		if (sample == null || sample.getSegments() == null || sample.getSegments().isEmpty()) {
			response.setStatusCode(ResponseStatus.MISSING_INPUT.getStatusCode());
			response.setStatusMessage(String.format(ResponseStatus.MISSING_INPUT.getStatusMessage(), "sample"));
			response.setResponse(null);
			return response;
		}
		Map<BiometricType, QualityScore> scores = new HashMap<>();
		Map<BiometricType, List<BIR>> segmentMap = getBioSegmentMap(sample, modalitiesToCheck);
		for (BiometricType modality : segmentMap.keySet()) {
			QualityScore qualityScore = evaluateQuality(modality, segmentMap.get(modality));
			scores.put(modality, qualityScore);
		}
		// int major =
		// Optional.ofNullable(sample.getBdbInfo()).map(BDBInfo::getQuality).map(QualityType::getScore)
		// .orElse(0L).intValue();
		response.setStatusCode(ResponseStatus.SUCCESS.getStatusCode());
		response.setStatusMessage(ResponseStatus.SUCCESS.getStatusMessage());
		QualityCheck check = new QualityCheck();
		check.setScores(scores);
		response.setResponse(check);
		return response;
	}

	private QualityScore evaluateQuality(BiometricType modality, List<BIR> segments) {
		QualityScore score = new QualityScore();
		List<String> errors = new ArrayList<>();
		score.setScore(0);
		switch (modality) {
		case FACE:
			return evaluateFaceQuality(segments);
		case FINGER:
			return evaluateFingerprintQuality(segments);
		case IRIS:
			return evaluateIrisQuality(segments);
		default:
			errors.add("Modality " + modality.name() + " is not supported");
		}
		score.setErrors(errors);
		return score;
	}

	private QualityScore evaluateFingerprintQuality(List<BIR> segments) {
		QualityScore score = new QualityScore();
		List<String> errors = new ArrayList<>();

		score.setScore(getAvgQualityScore(segments));

		// TODO actual quality evaluation here

		score.setErrors(errors);
		return score;
	}

	private float getAvgQualityScore(List<BIR> segments) {
		float qualityScore = 0;
		for (BIR bir : segments) {

			qualityScore += (bir.getBdbInfo().getQuality().getScore());
		}

		return qualityScore / segments.size();
	}

	private QualityScore evaluateIrisQuality(List<BIR> segments) {
		QualityScore score = new QualityScore();
		List<String> errors = new ArrayList<>();
		score.setScore(getAvgQualityScore(segments));

		// TODO actual quality evaluation here

		score.setErrors(errors);
		return score;
	}

	private QualityScore evaluateFaceQuality(List<BIR> segments) {
		QualityScore score = new QualityScore();
		List<String> errors = new ArrayList<>();
		score.setScore(getAvgQualityScore(segments));

		// TODO actual quality evaluation here

		score.setErrors(errors);
		return score;
	}

	@Override
	public Response<MatchDecision[]> match(BiometricRecord sample, BiometricRecord[] gallery,
			List<BiometricType> modalitiesToMatch, Map<String, String> flags) {
		if (true)
			return doMatch(sample, gallery, modalitiesToMatch, flags);
		MatchDecision matchingScore[] = new MatchDecision[gallery.length];
		int count = 0;
		Map<BiometricType, List<BIR>> sampleBioSegmentMap = getBioSegmentMap(sample, modalitiesToMatch);
		for (BiometricRecord recorded : gallery) {
			Map<BiometricType, List<BIR>> recordBioSegmentMap = getBioSegmentMap(recorded, modalitiesToMatch);
			Map<BiometricType, Decision> decision = new HashMap<>();
			matchingScore[count] = new MatchDecision(count);
			matchingScore[count].setGalleryIndex(count);

			/*
			 * if (Objects.nonNull(recordedValue) && Objects.nonNull(recordedValue.getBdb())
			 * && recordedValue.getBdb().length != 0 &&
			 * Arrays.equals(recordedValue.getBdb(), sample.getBdb())) {
			 * matchingScore[count].setDecisions(decisions); } else {
			 * matchingScore[count].setMatch(false); }
			 */
			modalitiesToMatch.forEach(type -> {
				Decision d = new Decision();
				d.setMatch(Match.MATCHED);
				decision.put(type, d);
			});
			matchingScore[count].setDecisions(decision);
			count++;
		}
		Response<MatchDecision[]> response = new Response<>();
		response.setStatusCode(200);
		response.setResponse(matchingScore);
		return response;
	}

	private Response<MatchDecision[]> doMatch(BiometricRecord sample, BiometricRecord[] gallery,
			List<BiometricType> modalitiesToMatch, Map<String, String> flags) {
		int index = 0;
		MatchDecision matchDecision[] = new MatchDecision[gallery.length];
		Response<MatchDecision[]> response = new Response<>();

		// Group Segments by modality
		Map<BiometricType, List<BIR>> sampleBioSegmentMap = getBioSegmentMap(sample, modalitiesToMatch);
		for (BiometricRecord record : gallery) {
			Map<BiometricType, List<BIR>> recordBioSegmentMap = getBioSegmentMap(record, modalitiesToMatch);
			matchDecision[index] = new MatchDecision(index);
			Map<BiometricType, Decision> decisions = new HashMap<>();
			Decision decision = new Decision();
			LOGGER.info("Comparing sample with gallery index " + index + " ----------------------------------");
			for (BiometricType modality : sampleBioSegmentMap.keySet()) {
				try {
					decision = compareModality(modality, sampleBioSegmentMap.get(modality),
							recordBioSegmentMap.get(modality));
				} catch (NoSuchAlgorithmException | NullPointerException ex) {
					ex.printStackTrace();
					decision.setMatch(Match.ERROR);
					decision.getErrors().add("Modality " + modality.name() + " threw an exception:" + ex.getMessage());
				} finally {
					decisions.put(modality, decision);
				}
			}
			matchDecision[index].setDecisions(decisions);
			index++;
		}

		response.setStatusCode(200);
		response.setResponse(matchDecision);
		return response;
	}

	private Decision compareModality(BiometricType modality, List<BIR> sampleSegments, List<BIR> gallerySegments)
			throws NoSuchAlgorithmException {
		Decision decision = new Decision();
		decision.setMatch(Match.ERROR);
		switch (modality) {
		case FACE:
			return compareFaces(sampleSegments, gallerySegments);
		case FINGER:
			return compareFingerprints(sampleSegments, gallerySegments);
		case IRIS:
			return compareIrises(sampleSegments, gallerySegments);
		default:
			// unsupported modality
			// TODO handle error status code here
			decision.setAnalyticsInfo(new HashMap<>());
			decision.getAnalyticsInfo().put("errors", "Modality " + modality.name() + " is not supported.");
		}
		return decision;
	}

	private Decision compareFingerprints(List<BIR> sampleSegments, List<BIR> gallerySegments)
			throws NoSuchAlgorithmException {
		List<String> errors = new ArrayList<>();
		List<Boolean> matched = new ArrayList<>();
		Decision decision = new Decision();
		decision.setMatch(Match.ERROR);

		if (sampleSegments == null && gallerySegments == null) {
			LOGGER.info("Modality: {} -- no biometrics found", BiometricType.FINGER.value());
			decision.setMatch(Match.MATCHED);
			return decision;
		} else if (sampleSegments == null || gallerySegments == null) {
			LOGGER.info("Modality: {} -- biometric missing in either sample or recorded", BiometricType.FINGER.value());
			decision.setMatch(Match.NOT_MATCHED);
			return decision;
		}

		for (BIR sampleBIR : sampleSegments) {
			Boolean bio_found = false;
			if (sampleBIR.getBdbInfo().getSubtype() != null && sampleBIR.getBdbInfo().getSubtype().get(0) != null
					&& !sampleBIR.getBdbInfo().getSubtype().get(0).isEmpty()
					&& !sampleBIR.getBdbInfo().getSubtype().get(0).contains("UNKNOWN")) {
				for (BIR galleryBIR : gallerySegments) {
					if (galleryBIR.getBdbInfo().getSubtype().get(0)
							.equals(sampleBIR.getBdbInfo().getSubtype().get(0))) {
						if (Util.compareHash(galleryBIR.getBdb(), sampleBIR.getBdb())) {
							LOGGER.info("Modality: {}; Subtype: {}  -- matched", BiometricType.FINGER.value(),
									galleryBIR.getBdbInfo().getSubtype());
							matched.add(true);
							bio_found = true;
						} else {
							LOGGER.info("Modality: {}; Subtype: {}  -- not matched", BiometricType.FINGER.value(),
									galleryBIR.getBdbInfo().getSubtype());
							matched.add(false);
							bio_found = true;
						}
					}
				}
			} else {
				for (BIR galleryBIR : gallerySegments) {
					if (Util.compareHash(galleryBIR.getBdb(), sampleBIR.getBdb())) {
						LOGGER.info("Modality: {}; Subtype: {}  -- matched", BiometricType.FINGER.value(),
								galleryBIR.getBdbInfo().getSubtype());
						matched.add(true);
						bio_found = true;
					} else {
						LOGGER.info("Modality: {}; Subtype: {}  -- not matched", BiometricType.FINGER.value(),
								galleryBIR.getBdbInfo().getSubtype());
						matched.add(false);
						bio_found = true;
					}
				}
			}
			if (!bio_found) {
				LOGGER.info("Modality: {}; Subtype: {} -- not found", BiometricType.FINGER.value(),
						sampleBIR.getBdbInfo().getSubtype());
				matched.add(false);
			}
		}

		int trueMatchCount = matched.stream().filter(val -> val == true).collect(Collectors.toList()).size();
		if (matched.size() > 0) {
			if (trueMatchCount == sampleSegments.size()) {
				decision.setMatch(Match.MATCHED);
			} else {
				decision.setMatch(Match.NOT_MATCHED);
			}
		} else {
			// TODO check the condition: what if no similar type and subtype found
			decision.setMatch(Match.ERROR);
		}
		return decision;
	}

	private Decision compareIrises(List<BIR> sampleSegments, List<BIR> gallerySegments)
			throws NoSuchAlgorithmException {
		List<String> errors = new ArrayList<>();
		List<Boolean> matched = new ArrayList<>();
		Decision decision = new Decision();
		decision.setMatch(Match.ERROR);

		if (sampleSegments == null && gallerySegments == null) {
			LOGGER.info("Modality: {} -- no biometrics found", BiometricType.IRIS.value());
			decision.setMatch(Match.MATCHED);
			return decision;
		} else if (sampleSegments == null || gallerySegments == null) {
			LOGGER.info("Modality: {} -- biometric missing in either sample or recorded", BiometricType.IRIS.value());
			decision.setMatch(Match.NOT_MATCHED);
			return decision;
		}

		for (BIR sampleBIR : sampleSegments) {
			Boolean bio_found = false;
			if (sampleBIR.getBdbInfo().getSubtype() != null && sampleBIR.getBdbInfo().getSubtype().get(0) != null
					&& !sampleBIR.getBdbInfo().getSubtype().get(0).isEmpty()
					&& !sampleBIR.getBdbInfo().getSubtype().get(0).contains("UNKNOWN")) {
				for (BIR galleryBIR : gallerySegments) {
					if (galleryBIR.getBdbInfo().getSubtype().get(0)
							.equals(sampleBIR.getBdbInfo().getSubtype().get(0))) {
						if (Util.compareHash(galleryBIR.getBdb(), sampleBIR.getBdb())) {
							LOGGER.info("Modality: {}; Subtype: {} -- matched", BiometricType.IRIS.value(),
									galleryBIR.getBdbInfo().getSubtype().get(0));
							matched.add(true);
							bio_found = true;
						} else {
							LOGGER.info("Modality: {}; Subtype: {} -- not matched", BiometricType.IRIS.value(),
									galleryBIR.getBdbInfo().getSubtype().get(0));
							matched.add(false);
							bio_found = true;
						}
					}
				}
			} else {
				for (BIR galleryBIR : gallerySegments) {
					if (Util.compareHash(galleryBIR.getBdb(), sampleBIR.getBdb())) {
						LOGGER.info("Modality: {}; Subtype: {} -- matched", BiometricType.IRIS.value(),
								galleryBIR.getBdbInfo().getSubtype());
						matched.add(true);
						bio_found = true;
					} else {
						LOGGER.info("Modality: {}; Subtype: {}-- not matched", BiometricType.IRIS.value(),
								galleryBIR.getBdbInfo().getSubtype());
						matched.add(false);
						bio_found = true;
					}
				}
			}
			if (!bio_found) {
				LOGGER.info("Modality: {} ; Subtype: {}  -- not found", BiometricType.IRIS.value(),
						sampleBIR.getBdbInfo().getSubtype());
				matched.add(false);
			}
		}
		if (matched.size() > 0) {
			if (matched.contains(true)) {
				decision.setMatch(Match.MATCHED);
			} else {
				decision.setMatch(Match.NOT_MATCHED);
			}
		} else {
			// TODO check the condition: what if no similar type and subtype found
			decision.setMatch(Match.ERROR);
		}
		return decision;
	}

	private Decision compareFaces(List<BIR> sampleSegments, List<BIR> gallerySegments) throws NoSuchAlgorithmException {
		List<String> errors = new ArrayList<>();
		List<Boolean> matched = new ArrayList<>();
		Decision decision = new Decision();
		decision.setMatch(Match.ERROR);

		if (sampleSegments == null && gallerySegments == null) {
			LOGGER.info("Modality: {} -- no biometrics found", BiometricType.FACE.value());
			decision.setMatch(Match.MATCHED);
			return decision;
		} else if (sampleSegments == null || gallerySegments == null) {
			LOGGER.info("Modality: {} -- biometric missing in either sample or recorded", BiometricType.FACE.value());
			decision.setMatch(Match.NOT_MATCHED);
			return decision;
		}

		for (BIR sampleBIR : sampleSegments) {
			Boolean bio_found = false;
			if (sampleBIR.getBdbInfo().getSubtype() != null && sampleBIR.getBdbInfo().getSubtype().get(0) != null
					&& !sampleBIR.getBdbInfo().getSubtype().get(0).isEmpty()) {
				for (BIR galleryBIR : gallerySegments) {
					if (galleryBIR.getBdbInfo().getSubtype().get(0)
							.equals(sampleBIR.getBdbInfo().getSubtype().get(0))) {
						if (Util.compareHash(galleryBIR.getBdb(), sampleBIR.getBdb())) {
							LOGGER.info("Modality: {}; Subtype: {} -- matched", BiometricType.FACE.value(),
									galleryBIR.getBdbInfo().getSubtype().get(0));
							matched.add(true);
							bio_found = true;
						} else {
							LOGGER.info("Modality: {}; Subtype: {} -- not matched", BiometricType.FACE.value(),
									galleryBIR.getBdbInfo().getSubtype().get(0));
							matched.add(false);
							bio_found = true;
						}
					}
				}
			} else {
				for (BIR galleryBIR : gallerySegments) {
					if (Util.compareHash(galleryBIR.getBdb(), sampleBIR.getBdb())) {
						LOGGER.info("Modality: {}; Subtype: {} -- matched", BiometricType.FACE.value(),
								galleryBIR.getBdbInfo().getSubtype());
						matched.add(true);
						bio_found = true;
					} else {
						LOGGER.info("Modality: {}; Subtype: {} -- not matched", BiometricType.FACE.value(),
								galleryBIR.getBdbInfo().getSubtype());
						matched.add(false);
						bio_found = true;
					}
				}
			}
			if (!bio_found) {
				LOGGER.info("Modality: {}; Subtype: {} -- not found", BiometricType.FACE.value(),
						sampleBIR.getBdbInfo().getSubtype());
				matched.add(false);
			}
		}
		if (matched.size() > 0) {
			if (!matched.contains(false)) {
				decision.setMatch(Match.MATCHED);
			} else {
				decision.setMatch(Match.NOT_MATCHED);
			}
		} else {
			// TODO check the condition: what if no similar type and subtype found
			decision.setMatch(Match.ERROR);
		}
		return decision;
	}

	private Map<BiometricType, List<BIR>> getBioSegmentMap(BiometricRecord record,
			List<BiometricType> modalitiesToMatch) {
		Boolean noFilter = false;
		// if the modalities to match is not passed, assume that all modalities have to
		// be matched.
		if (modalitiesToMatch == null || modalitiesToMatch.isEmpty())
			noFilter = true;

		Map<BiometricType, List<BIR>> bioSegmentMap = new HashMap<>();
		for (BIR segment : record.getSegments()) {
			BiometricType bioType = segment.getBdbInfo().getType().get(0);

			// ignore modalities that are not to be matched
			if (noFilter == false && !modalitiesToMatch.contains(bioType))
				continue;

			if (!bioSegmentMap.containsKey(bioType)) {
				bioSegmentMap.put(bioType, new ArrayList<BIR>());
			}
			bioSegmentMap.get(bioType).add(segment);
		}

		return bioSegmentMap;
	}

	@Override
	public Response<BiometricRecord> extractTemplate(BiometricRecord sample, List<BiometricType> modalitiesToExtract,
			Map<String, String> flags) {
		Response<BiometricRecord> response = new Response<>();
		response.setStatusCode(200);
		response.setResponse(sample);
		return response;
	}

	@Override
	public Response<BiometricRecord> convertFormat(BiometricRecord record, String sourceFormat, String targetFormat,
			Map<String, String> sourceParams, Map<String, String> targetParams,
			List<BiometricType> modalitiesToConvert) {
		Response<BiometricRecord> response = new Response<>();
		Map<String, String> values = new HashMap<>();
		for (BIR segment : record.getSegments()) {
			BiometricType bioType = segment.getBdbInfo().getType().get(0);
			List<String> bioSubTypeList = segment.getBdbInfo().getSubtype();
			String bioSubType = "";
			if (bioSubTypeList != null && !bioSubTypeList.isEmpty())
				bioSubType = bioSubTypeList.get(0);

			String key = bioType + "_" + bioSubType;
			// ignore modalities that are not to be matched
			if (!isValidBiometricType(bioType, sourceFormat))
				continue;

			if (!values.containsKey(key)) {
				values.put(key, encodeToURLSafeBase64(segment.getBdb()));
			}
		}

		Map<String, String> responseValues = null;
		try {
			responseValues = new ConverterServiceImpl().convert(values, sourceFormat, targetFormat, sourceParams,
					targetParams);
			List<BIR> birList = record.getSegments();
			for (int index = 0; index < birList.size(); index++) {
				BIR segment = birList.get(index);
				BiometricType bioType = segment.getBdbInfo().getType().get(0);
				List<String> bioSubTypeList = segment.getBdbInfo().getSubtype();
				String bioSubType = "";
				if (bioSubTypeList != null && !bioSubTypeList.isEmpty())
					bioSubType = bioSubTypeList.get(0);

				String key = bioType + "_" + bioSubType;
				// ignore modalities that are not to be matched
				if (!isValidBiometricType(bioType, sourceFormat))
					continue;

				if (responseValues != null && responseValues.containsKey(key)) {
					segment.getBirInfo().setPayload(segment.getBdb());
					segment.setBdb(decodeURLSafeBase64(responseValues.get(key)));
				}
				birList.set(index, segment);
			}
			record.setSegments(birList);
			response.setStatusCode(200);
			response.setResponse(record);
		} catch (ConversionException ex) {
			LOGGER.error("convertFormat -- error", ex);
			switch (ConverterErrorCode.fromErrorCode(ex.getErrorCode())) {
			case INPUT_SOURCE_EXCEPTION:
			case INVALID_REQUEST_EXCEPTION:
			case INVALID_SOURCE_EXCEPTION:
			case INVALID_TARGET_EXCEPTION:
			case SOURCE_NOT_VALID_FINGER_ISO_FORMAT_EXCEPTION:
			case SOURCE_NOT_VALID_FACE_ISO_FORMAT_EXCEPTION:
			case SOURCE_NOT_VALID_IRIS_ISO_FORMAT_EXCEPTION:
			case SOURCE_NOT_VALID_BASE64URLENCODED_EXCEPTION:
			case COULD_NOT_READ_ISO_IMAGE_DATA_EXCEPTION:
			case TARGET_FORMAT_EXCEPTION:
			case NOT_SUPPORTED_COMPRESSION_TYPE:
				response.setStatusCode(401);
				response.setResponse(null);
				break;

			case SOURCE_CAN_NOT_BE_EMPTY_OR_NULL_EXCEPTION:
				response.setStatusCode(404);
				response.setResponse(null);
				break;

			default:
				response.setStatusCode(500);
				response.setResponse(null);
				break;
			}
		} catch (Exception ex) {
			LOGGER.error("convertFormat -- error", ex);
			response.setStatusCode(500);
			response.setResponse(null);
		}

		return response;
	}

	private boolean isValidBiometricType(BiometricType bioType, String sourceFormat) {
		boolean isValid = false;
		switch (sourceFormat) {
		case "ISO19794_4_2011":
			if (bioType == BiometricType.FINGER)
				isValid = true;
			break;
		case "ISO19794_5_2011":
			if (bioType == BiometricType.FACE)
				isValid = true;
			break;
		case "ISO19794_6_2011":
			if (bioType == BiometricType.IRIS)
				isValid = true;
			break;
		}
		return isValid;
	}

	@Override
	public Response<BiometricRecord> segment(BiometricRecord sample, List<BiometricType> modalitiesToSegment,
			Map<String, String> flags) {
		BiometricRecord record = new BiometricRecord();
		record.setSegments(null);
		Response<BiometricRecord> response = new Response<>();
		response.setStatusCode(200);
		response.setResponse(record);
		return response;
	}

	private static Encoder urlSafeEncoder;

	static {
		urlSafeEncoder = Base64.getUrlEncoder().withoutPadding();
	}

	public static String encodeToURLSafeBase64(byte[] data) {
		if (isNullEmpty(data)) {
			return null;
		}
		return urlSafeEncoder.encodeToString(data);
	}

	public static String encodeToURLSafeBase64(String data) {
		if (isNullEmpty(data)) {
			return null;
		}
		return urlSafeEncoder.encodeToString(data.getBytes(StandardCharsets.UTF_8));
	}

	public static byte[] decodeURLSafeBase64(String data) {
		if (isNullEmpty(data)) {
			return null;
		}
		return Base64.getUrlDecoder().decode(data);
	}

	public static boolean isNullEmpty(byte[] array) {
		return array == null || array.length == 0;
	}

	public static boolean isNullEmpty(String str) {
		return str == null || str.trim().length() == 0;
	}
}
