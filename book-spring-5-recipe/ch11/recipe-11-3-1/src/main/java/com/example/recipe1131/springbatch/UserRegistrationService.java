package com.example.recipe1131.springbatch;

import java.util.Collection;
import java.util.Date;

public interface UserRegistrationService {

    Collection<UserRegistration> getOutstandingUserRegistrationBatchForDate(int quantity, Date date);
    UserRegistration registerUser(UserRegistration userRegistrationRegistration);
}
