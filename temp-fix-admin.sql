UPDATE users 
SET userpassword = '$2a$10$EH4s9bxxtZynR/d1/ZSBAORnaVw4JRN3abKR2uC51Fe8LnuBG.9yK',
    forcepasswordupdate = false
WHERE username = 'admin';

SELECT username, LEFT(userpassword, 20) as pwd_hash, userrole, enabled 
FROM users 
WHERE username = 'admin';
