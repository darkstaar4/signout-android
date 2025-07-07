# Login Simplification

## Problem
- User couldn't login with username "racex" (got "no account found") 
- User couldn't login with email "racexcars@gmail.com" (login screen stayed stuck)
- Complex username-to-email mapping system was causing issues

## Solution
**Simplified login to only accept email addresses**

### Changes Made

1. **CognitoAuthService.kt** - Modified login logic:
   ```kotlin
   // OLD: Complex username lookup with AWS Admin API
   val loginIdentifier = if (username.contains("@")) {
       username
   } else {
       findUserByPreferredUsername(username) ?: username
   }
   
   // NEW: Simple email-only login
   val loginIdentifier = if (username.contains("@")) {
       username
   } else {
       return AuthResult(
           isSuccess = false,
           error = "Please use your email address to log in."
       )
   }
   ```

2. **Updated UI to reflect email-only login**:
   - Changed login field label from "Enter your details" to "Email address"
   - Changed placeholder from "Username" to "Email address" 
   - Updated ContentType from `Username` to `EmailAddress`
   - Added `common_email_address` string resource
   - Updated both LoginPasswordView and OnBoardingView

3. **Removed complex functions**:
   - `findUserByPreferredUsername()`
   - `userExistsWithEmail()`
   - AWS SDK v2 dependencies
   - PreVerificationService files

4. **Reverted FTUE changes** - Removed pre-verification logic to keep it simple

## Testing Instructions

1. **Build the app** (when Java toolchain issues are resolved):
   ```bash
   ./gradlew assembleDebug
   ```

2. **Install the APK**:
   ```bash
   adb install -r app/build/outputs/apk/gplay/debug/app-gplay-universal-debug.apk
   ```

3. **Test login scenarios**:
   - ✅ **Should work**: `racexcars@gmail.com` + `Monster1`
   - ❌ **Should show error**: `racex` + `Monster1` → "Please use your email address to log in."

## User Data (for reference)
- **Email**: racexcars@gmail.com (Cognito username)
- **Password**: Monster1
- **preferred_username**: racex
- **Matrix credentials**: racexcars / Monster1
- **Status**: CONFIRMED

## Next Steps
1. Resolve Java toolchain issues to build the app
2. Test with the simplified email-only login
3. User should login with `racexcars@gmail.com` instead of `racex` 