package com.protei.task.systemuser;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber.PhoneNumber;
import com.protei.task.exception.UserNotFoundException;
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

@Service
public class SystemUserService {
    private static final String PHONE_NUMBER_REGION = "RU";

    private static final String NAME_IS_INVALID = "User name is invalid!";
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

    public SystemUser getSystemUserById(long userId) {
        return findSystemUserById(userId);
    }

    public SystemUser addNewSystemUser(SystemUser user) {
        validateName(user.getName());
        validateEmail(user.getEmail());
        validatePhoneNumber(user.getPhoneNumber());
        user.setUserStatus(SystemUserStatus.OFFLINE);
        return systemUserRepository.save(user);
    }

    public void deleteSystemUser(long userId) {
        SystemUser user = findSystemUserById(userId);
        systemUserRepository.delete(user);
    }

    @Transactional
    public void updateSystemUser(long userId, String name, String email, String phoneNumber) {
        SystemUser user = findSystemUserById(userId);
        if (name != null) {
            validateName(name);
            user.setName(name);
        }
        if (email != null) {
            validateEmail(email);
            user.setEmail(email);
        }
        if (phoneNumber != null) {
            validatePhoneNumber(phoneNumber);
            user.setPhoneNumber(phoneNumber);
        }
    }

    @Transactional
    public SystemUser updateSystemUserStatus(long userId, String userStatus) {
        SystemUser user = findSystemUserById(userId);
        validateStatus(userStatus);
        SystemUserStatus status = SystemUserStatus.valueOf(userStatus);
        user.setUserStatus(status);
        handleUserStatusJob(userId, status);
        return user;
    }

    private SystemUser findSystemUserById(long userId) {
        return systemUserRepository.findSystemUserById(userId)
                .orElseThrow(() -> new UserNotFoundException(String.format(NO_USER_WITH_ID, userId)));
    }

    private void validateName(String name) {
        if (name.length() == 0 || name.length() > 20) {
            throw new UserValidationException(NAME_IS_INVALID);
        }
    }

    private void validatePhoneNumber(String phoneNumber) {
        if (isPhoneNumberInvalid(phoneNumber)) {
            throw new UserValidationException(PHONE_NUMBER_IS_INVALID);
        }
        if (isPhoneNumberTaken(phoneNumber)) {
            throw new UserValidationException(PHONE_NUMBER_IS_TAKEN);
        }
    }

    private void validateEmail(String email) {
        if (isEmailInvalid(email)) {
            throw new UserValidationException(EMAIL_IS_INVALID);
        }
        if (isEmailTaken(email)) {
            throw new UserValidationException(EMAIL_IS_TAKEN);
        }
    }

    private void validateStatus(String status) {
        try {
            SystemUserStatus.valueOf(status);
        } catch (IllegalArgumentException e) {
            throw new UserValidationException(USER_STATUS_IS_INVALID);
        }
    }

    private boolean isPhoneNumberTaken(String phoneNumber) {
        return systemUserRepository.findSystemUserByPhoneNumber(phoneNumber).isPresent();
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
        return systemUserRepository.findSystemUserByEmail(email).isPresent();
    }

    private boolean isEmailInvalid(String email) {
        return !EmailValidator.getInstance().isValid(email);
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
