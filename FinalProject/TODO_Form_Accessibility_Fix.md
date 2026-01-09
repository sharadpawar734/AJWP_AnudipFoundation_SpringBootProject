# Form Accessibility Fix - TODO List

## Task
Fix form field accessibility issues by adding:
1. Autocomplete attributes to form fields
2. ID attributes to inputs where missing
3. Proper label associations using `for` attributes

## Files to Fix

### HTML Templates
- [ ] `src/main/resources/templates/Signup.html` - Add autocomplete attributes and labels to all form fields
- [ ] `src/main/resources/templates/Login.html` - Add autocomplete attributes to email and password
- [ ] `src/main/resources/templates/Checkout.html` - Add autocomplete attributes to billing and payment fields
- [ ] `src/main/resources/templates/Profile.html` - Add autocomplete attributes to profile form fields
- [ ] `src/main/resources/templates/admin-login.html` - Add autocomplete attributes to admin login fields

## Autocomplete Values to Use
- `username` → `username` or `name`
- `email` → `email`
- `password` → `current-password` or `new-password`
- `phone` → `tel`
- `address` → `street-address`
- `city` → `address-level2`
- `state` → `address-level1`
- `pincode` → `postal-code`
- `cardNumber` → `cc-number`
- `cardExpiry` → `cc-exp`
- `cardCvv` → `cc-csc`
- `upiId` → `tel` or custom

## Progress
- [x] Create TODO list
- [x] Fix Signup.html
- [x] Fix Login.html
- [x] Fix Checkout.html
- [x] Fix Profile.html
- [x] Fix admin-login.html
- [x] Verify all fixes

## Started: 2024
## Status: COMPLETED

