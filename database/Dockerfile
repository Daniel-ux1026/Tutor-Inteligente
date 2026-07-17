FROM mcr.microsoft.com/mssql/server:2022-latest

USER root
COPY database/railway-entrypoint.sh /usr/local/bin/railway-entrypoint.sh
RUN chmod 0755 /usr/local/bin/railway-entrypoint.sh

USER root
EXPOSE 1433

ENTRYPOINT ["/usr/local/bin/railway-entrypoint.sh"]
