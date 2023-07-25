FROM minih-jre
MAINTAINER hub@minih.cn
EXPOSE 8090

ENV minih.core.aliyunApiKey=LTAI5tGfUXxsnrC9tf2CgQj2
ENV minih.core.aliyunApiSecret=ZL96vjn9LuAx5lxumaf9e3LdJ1jhAg
ENV mongodb.host=dds-wz9019cf0593d9141601-pub.mongodb.rds.aliyuncs.com
ENV mongodb.password=Minih@123
ENV mongodb.source=admin
ENV mongodb.username=root
ENV redis.connectionString=redis://:Minih123@minih001.redis.rds.aliyuncs.com:6379/0

WORKDIR /projects

ADD ./build/install/minih-vertx-system/ /projects

ENTRYPOINT ["cd bin","minih-vertx-system"]
