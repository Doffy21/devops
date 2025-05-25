# Vérifie que le conteneur ne tourne pas en root
control 'no-root-user' do
  impact 1.0
  title 'Le conteneur ne doit pas tourner en tant que root'
  describe command('id -u') do
    its('stdout.to_i') { should_not eq 0 }
  end
end

# Vérifie que le port 8080 est ouvert (backend)
control 'quarkus-port' do
  impact 0.7
  title 'Port 8080 doit être ouvert'
  describe port(8080) do
    it { should be_listening }
  end
end

# Vérifie que le fichier de configuration n'est pas modifiable par tout le monde
control 'application-properties-permissions' do
  impact 0.5
  title 'application.properties doit être sécurisé'
  describe file('/app/src/main/resources/application.properties') do
    it { should_not be_writable.by('others') }
    it { should exist }
  end
end
