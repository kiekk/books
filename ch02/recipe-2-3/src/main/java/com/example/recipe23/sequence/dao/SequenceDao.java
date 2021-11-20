package com.example.recipe23.sequence.dao;

import com.example.recipe23.sequence.entity.Sequence;

public interface SequenceDao {
    Sequence getSequence(String sequenceId);

    int getNextValue(String sequenceId);
}
