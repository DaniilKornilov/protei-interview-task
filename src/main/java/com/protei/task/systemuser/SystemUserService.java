package com.protei.task.systemuser;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber.PhoneNumber;
import com.protei.task.exception.UserValidationException;
import com.protei.task.scheduler.JobInfo;
import com.protei.task.scheduler.SchedulerService;
import com.protei.task.scheduler.SystemUserStatusUpdateJob;
import com.protei.task.systemuser.enumeration.SystemUserStatus;
import org.apache.commons.validator.routines.EmailValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.List;
import java.util.Optional;

@Service
public class SystemUserService {
    private static final String PHONE_NUMBER_REGION = "RU";

    private static final String PHONE_NUMBER_IS_TAKEN = "Phone number is taken!";
    private static final String EMAIL_IS_TAKEN = "Email is taken!";
    private static final String EMAIL_IS_INVALID = "Email is invalid!";
    private static final String PHONE_NUMBER_IS_INVALID = "Phone number is invalid!";
    private static final String NO_USER_WITH_ID = "No user with id: %s!";
    private static final String USER_STATUS_IS_INVALID = "User status is invalid!";

    //5 minutes
    private static final long TIME_TO_SET_AWAY_STATUS = 300000L;

    private final SystemUserRepository systemUserRepository;
    private final SchedulerService schedulerService;

    @Autowired
    public SystemUserService(SystemUserRepository systemUserRepository, SchedulerService schedulerService) {
        this.systemUserRepository = systemUserRepository;
        this.schedulerService = schedulerService;
    }

    public List<SystemUser> getSystemUsers() {
        return systemUserRepository.findAll();
    }

    public SystemUser getSystemUSerById(long userId) {
        return systemUserRepository.findSystemUserById(userId)
                .orElseThrow(() ->
                        new UserValidationException(String.format(NO_USER_WITH_ID, userId)));
    }

    public JsonNode addNewSystemUser(SystemUser user) {
        if (isEmailInvalid(user.getEmail())) {
            throw new UserValidationException(EMAIL_IS_INVALID);
        }
        if (isPhoneNumberInvalid(user.getPhoneNumber())) {
            throw new UserValidationException(PHONE_NUMBER_IS_INVALID);
        }
        if (isEmailTaken(user.getEmail())) {
            throw new UserValidationException(EMAIL_IS_TAKEN);
        }
        if (isPhoneNumberTaken(user.getPhoneNumber())) {
            throw new UserValidationException(PHONE_NUMBER_IS_TAKEN);
        }
        user.setUserStatus(SystemUserStatus.OFFLINE);
        long userId = systemUserRepository.save(user).getId();
        return formJsonForAddNewSystemUser(userId);
    }

    public void deleteSystemUser(long userId) {
        if (!systemUserRepository.existsById(userId)) {
            throw new UserValidationException(String.format(NO_USER_WITH_ID, userId));
        }
        systemUserRepository.deleteById(userId);
    }

    @Transactional
    public void updateSystemUser(long userId, String name, String email, String phoneNumber) {
        Optional<SystemUser> userOptional = systemUserRepository.findSystemUserById(userId);
        if (userOptional.isEmpty()) {
            throw new UserValidationException(String.format(NO_USER_WITH_ID, userId));
        }
        SystemUser user = userOptional.get();
        if (name != null && name.length() > 0) {
            user.setName(name);
        }

        if (email != null) {
            if (isEmailInvalid(email)) {
                throw new UserValidationException(EMAIL_IS_INVALID);
            }
            if (isEmailTaken(email)) {
                throw new UserValidationException(EMAIL_IS_TAKEN);
            }
            user.setEmail(email);
        }

        if (phoneNumber != null) {
            if (isPhoneNumberInvalid(phoneNumber)) {
                throw new UserValidationException(PHONE_NUMBER_IS_INVALID);
            }
            if (isPhoneNumberTaken(phoneNumber)) {
                throw new UserValidationException(PHONE_NUMBER_IS_TAKEN);
            }
            user.setPhoneNumber(phoneNumber);
        }
    }

    @Transactional
    public JsonNode updateSystemUserStatus(long userId, String userStatus) {
        Optional<SystemUser> userOptional = systemUserRepository.findSystemUserById(userId);
        if (userOptional.isEmpty()) {
            throw new UserValidationException(String.format(NO_USER_WITH_ID, userId));
        }
        SystemUser user = userOptional.get();
        SystemUserStatus previousStatus = user.getUserStatus();
        SystemUserStatus status;
        try {
            status = SystemUserStatus.valueOf(userStatus);
        } catch (IllegalArgumentException e) {
            throw new UserValidationException(USER_STATUS_IS_INVALID);
        }
        user.setUserStatus(status);

        handleUserStatusJob(userId, status);

        return formJsonForSystemUserStatusUpdate(userId, previousStatus, user.getUserStatus());
    }

    private boolean isPhoneNumberTaken(String phoneNumber) {
        Optional<SystemUser> systemUserOptional =
                systemUserRepository.findSystemUserByPhoneNumber(phoneNumber);
        return systemUserOptional.isPresent();
    }

    private boolean isPhoneNumberInvalid(String phoneNumber) {
        PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();
        PhoneNumber phoneNumberProto;
        try {
            phoneNumberProto = phoneUtil.parse(phoneNumber, PHONE_NUMBER_REGION);
        } catch (NumberParseException e) {
            throw new UserValidationException(e.getMessage());
        }
        return !phoneUtil.isValidNumber(phoneNumberProto);
    }

    private boolean isEmailTaken(String email) {
        Optional<SystemUser> systemUserOptional =
                systemUserRepository.findSystemUserByEmail(email);
        return systemUserOptional.isPresent();
    }

    private boolean isEmailInvalid(String email) {
        return !EmailValidator.getInstance().isValid(email);
    }

    private JsonNode formJsonForSystemUserStatusUpdate(long userId,
                                                       SystemUserStatus previousStatus,
                                                       SystemUserStatus currentStatus) {
        String jsonString = "{\"userId\":\"" + userId + "\"," +
                "\"previousUserStatus\":\"" + previousStatus + "\"," +
                "\"currentUserStatus\":\"" + currentStatus + "\"}";
        JsonNode jsonNode;
        try {
            jsonNode = new ObjectMapper().readTree(jsonString);
        } catch (JsonProcessingException e) {
            throw new UserValidationException(e.getMessage());
        }
        return jsonNode;
    }

    private JsonNode formJsonForAddNewSystemUser(long userId) {
        String jsonString = "{\"userId\":\"" + userId + "\"}";
        JsonNode jsonNode;
        try {
            jsonNode = new ObjectMapper().readTree(jsonString);
        } catch (JsonProcessingException e) {
            throw new UserValidationException(e.getMessage());
        }
        return jsonNode;
    }

    private void handleUserStatusJob(long userId, SystemUserStatus status) {
        String userIdString = String.valueOf(userId);
        if (status == SystemUserStatus.ONLINE) {
            runUpdateStatusJob(userIdString);
        }
        if (((status == SystemUserStatus.OFFLINE) || (status == SystemUserStatus.AWAY))
                && (schedulerService.getRunningJob(userIdString) != null)) {
            schedulerService.deleteJob(userIdString);
        }
    }

    private void runUpdateStatusJob(String userId) {
        if (schedulerService.getRunningJob(userId) != null) {
            schedulerService.deleteJob(userId);
        }
        scheduleTask(userId);
    }

    private void scheduleTask(String userId) {
        JobInfo info = new JobInfo();
        info.setUserId(userId);
        info.setInitialOffsetMs(TIME_TO_SET_AWAY_STATUS);
        schedulerService.schedule(SystemUserStatusUpdateJob.class, info);
    }
}
