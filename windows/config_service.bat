@echo off
echo JavaService.exe -install "Secure FTP Wrapper" %1 -Djava.class.path=%2 -Duser.dir=%3 -start com.glub.secureftp.wrapper.SecureFTPWrapper -stop com.glub.secureftp.wrapper.SecureFTPWrapper -method shutdown > install_service.bat 
del config_service.bat