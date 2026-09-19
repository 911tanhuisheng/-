package com.spingbootinit.model.dto.contest;

import lombok.Data;

import java.io.Serializable;

@Data
public class ContestJoinRequest implements Serializable {

    private Long contestId;
}
