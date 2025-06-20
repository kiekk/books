package com.example.recipe211.service;

import com.example.recipe211.entity.Sequence;

public interface SequenceService {
    Sequence getSequence(String sequenceId);

    int getNextValue(String sequenceId);
}
