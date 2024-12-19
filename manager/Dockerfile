FROM python:3.11

COPY src/ /app/
COPY requirements.txt /app/requirements.txt

RUN pip install -r /app/requirements.txt

EXPOSE 5000

ENTRYPOINT ["flask", "--app", "/app/app", "run", "--port", "5000", "--host", "0.0.0.0"]