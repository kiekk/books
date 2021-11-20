package com.example.recipe23.sequence.service;

import com.example.recipe23.sequence.dao.SequenceDao;
import com.example.recipe23.sequence.entity.Sequence;
import org.springframework.stereotype.Component;

@Component
public class SequenceService {

    private final SequenceDao sequenceDao;

    public SequenceService(SequenceDao sequenceDao) {
        this.sequenceDao = sequenceDao;
    }

    public String generate(String sequenceId) {
        Sequence sequence = sequenceDao.getSequence(sequenceId);
        int value = sequenceDao.getNextValue(sequenceId);
        return sequence.getPrefix() + value + sequence.getSuffix();
    }

}
