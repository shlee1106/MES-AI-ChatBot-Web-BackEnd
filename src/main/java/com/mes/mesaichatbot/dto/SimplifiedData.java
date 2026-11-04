package com.mes.mesaichatbot.dto;

import java.util.List;

/**
 * 청크에 불필요한 데이터를 정리한 형식입니다.
 *
 * @param rowNumber 행 번호입니다.
 * @param rowData   행에 들어간 실제 데이터입니다.
 */
public record SimplifiedData(int dataId, int rowNumber, String rowData) {
    /**
     * {@link UploadDataDetailsDto}로부터 새 SimplifiedData 객체를 만듭니다.
     *
     * @param uploadDataDetailsDto {@link UploadDataDetailsDto}
     * @return 새 {@link SimplifiedData} 객체.
     */
    public static SimplifiedData from(UploadDataDetailsDto uploadDataDetailsDto) {
        return new SimplifiedData(uploadDataDetailsDto.dataId(), uploadDataDetailsDto.rowNumber(),
                                  uploadDataDetailsDto.rowData());
    }

    /**
     * 여러 {@link UploadDataDetailsDto}로부터 새 SimplifiedData 객체들을 만듭니다.
     *
     * @param uploadDataDetailsDto {@link UploadDataDetailsDto}
     * @return 새 {@link SimplifiedData} 객체를 담은 {@link List}.
     */
    public static List<SimplifiedData> from(List<UploadDataDetailsDto> uploadDataDetailsDto) {
        return uploadDataDetailsDto.parallelStream().map(SimplifiedData::from).toList();
    }
}
