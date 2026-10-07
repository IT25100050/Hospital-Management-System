# Hospital-Management-System

## Pharmacy access

Only `ADMIN` and `PHARMACIST` can see and open the Pharmacy section. They can
add, edit, fulfill, and delete pharmacy records. The UI hides management controls
for other roles, and the backend restricts data changes to these two roles.

## Doctor & Medical Record Manager role

`DOCTOR_RECORDS_MANAGER` is a restricted role for managing doctor profiles,
reviewing doctor registration requests, viewing medical records, submitting record
edit requests, and directly deleting medical records. The assigned doctor must
approve requested edits before they take effect; managers can submit another edit
request once the previous request has been reviewed. Doctors can directly edit or
delete records assigned to their profile, and create records from their approved appointments.
A manager's pending request is automatically closed if the doctor directly changes
that record, so an outdated proposal cannot later overwrite the doctor's changes.
Deleting a record as a manager also closes any pending edit request for that record.
It is not a
full `ADMIN` role. It cannot access user
management, staff or department administration (other than read-only department
lookup for doctor profiles), appointments, billing management, laboratory
management, the system dashboard, or audit logs. The role can view the patient directory only to
select patients for medical records.

An existing `ADMIN` can assign this role from **Hospital Admin → Login Accounts →
Change role**, or create an account with the role directly from **Create account**.
`DOCTOR`
is a separate role and can only be assigned to an account with an approved doctor
profile. The system prevents demoting the last `ADMIN`; an administrator can
change their own role only when another full `ADMIN` will remain. Keep a separate
`ADMIN` account for system-wide administration and account-role changes.

The role is enforced on backend endpoints as well as in dashboard navigation.
Role changes take effect in newly issued login tokens, so the account holder should
sign out and sign back in after their role is changed.
On startup, a rerunnable migration converts the existing MySQL `users.role` ENUM
column to `VARCHAR(32)` so the additional role is accepted without losing existing
role values.

An `ADMIN` can delete another login account from **Hospital Admin → Login Accounts**.
The currently signed-in administrator and the last remaining `ADMIN` cannot be
deleted. Linked patient profiles and clinical history are retained, but the removed
account's email is cleared from its patient profile to prevent that history from being
relinked to a new account. A linked doctor profile is deactivated and unlinked.
Reviewer references are detached, and medical record change-request history retains
the requester's username.

## Bootstrap a full system administrator

For a fresh database only, an initial full `ADMIN` can be provisioned with
environment variables. This is a separate system-administration account, not the
restricted Doctor & Medical Record Manager account. Use a unique internal email
and enter the password at the secure prompt instead of storing it in source code:

```powershell
$env:HMS_BOOTSTRAP_ADMIN_USERNAME = "system-admin"
$env:HMS_BOOTSTRAP_ADMIN_EMAIL = "system-admin@medicore.invalid"
$credential = Get-Credential -UserName "system-admin" -Message "Enter a strong bootstrap admin password"
$env:HMS_BOOTSTRAP_ADMIN_PASSWORD = $credential.GetNetworkCredential().Password
.\mvnw.cmd spring-boot:run
```

Bootstrap creates an administrator only if none exists. Passwords are BCrypt
hashed. Clear the temporary environment values when finished:

```powershell
Remove-Item Env:HMS_BOOTSTRAP_ADMIN_USERNAME, Env:HMS_BOOTSTRAP_ADMIN_EMAIL, Env:HMS_BOOTSTRAP_ADMIN_PASSWORD
Remove-Variable credential
```

Configure the MySQL datasource in `src/main/resources/application.properties`
before starting the application. Run tests with `.\mvnw.cmd test`.
