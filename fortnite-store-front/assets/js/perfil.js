/**
 * Classe responsável por gerenciar a autenticação (Login e Cadastro).
 * Controla a interface, validação de formulários e comunicação com a API.
 */
class UserPerfil {
    constructor() {

        this.API_LOGIN = 'http://130.213.12.104:8080/api/v1/auth/login';
        this.API_CADASTRO = 'http://130.213.12.104:8080/api/v1/auth/cadastro';
        
        // Inicialização das variáveis de estado do formulário
        this.email = '';
        this.password = '';
        this.name = '';
        this.confirmPassword = '';

        // Inicia a captura dos elementos do DOM
        this.buscaBotoes();
    }

    // Mapeia os botões e elementos de erro no HTML
    buscaBotoes() {
        this.loginButton = document.getElementById('btn-login');
        this.registerButton = document.getElementById('btn-cadastro');
        
        // Elemento para exibir erros na tela de login
        this.errorElement = document.getElementById('error-message'); 

        // Salva o texto original dos botões para restaurar após o "loading"
        if (this.loginButton) {
            this.loginButton.originalText = this.loginButton.innerHTML;
        }
        if (this.registerButton) {
            this.registerButton.originalText = this.registerButton.innerHTML;
        }

        this.eventosBotoes();
    }

    eventosBotoes() {
        if (this.loginButton){
            this.loginButton.addEventListener('click', (e) => {
                e.preventDefault();
                
                this.email = document.getElementById('email').value.trim();
                this.password = document.getElementById('password').value.trim();
                
                if(!this.email || !this.password){
                    this.showLoginError('Email e senha são obrigatórios.');
                    return;
                }
                this.login();
            });
        }

        if (this.registerButton){
            this.registerButton.addEventListener('click', (e) => {
                e.preventDefault();
                
                this.email = document.getElementById('email').value.trim();
                this.password = document.getElementById('password').value.trim();
                this.name = document.getElementById('name').value.trim();
                this.confirmPassword = document.getElementById('confirm-password').value.trim();
                
                if(!this.email || !this.password || !this.name || !this.confirmPassword){
                    Swal.fire({
                        icon: 'warning',
                        title: 'Campos Vazios',
                        text: 'Por favor, preencha todos os campos.'
                    });
                    return;
                }
                this.register();
            });
        }
    }

    async register() {
        this.setLoading(true, this.registerButton);

        if (this.password !== this.confirmPassword) {
            Swal.fire({
                icon: 'error',
                title: 'Oops...',
                text: 'As senhas não coincidem.'
            });
            this.setLoading(false, this.registerButton);
            return;
        }

        const data = {
            email: this.email,
            senha: this.password,
            nome: this.name
        };

        try {

            const response = await fetch(this.API_CADASTRO, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(data)
            });
            const resultText = await response.text();


            if (!response.ok) {
                throw new Error(resultText || `Erro HTTP! Status: ${response.status}`);
            }
            

            await Swal.fire({
                icon: 'success',
                title: 'Cadastro realizado!',
                text: `${resultText} Você será redirecionado para o login.`,
                confirmButtonText: 'Entendido!'
            });
            
            window.location.href = 'login.html';

        } catch (error) {
            console.error('Falha no Cadastro:', error);

            Swal.fire({
                icon: 'error',
                title: 'Falha no Cadastro',
                text: error.message || 'Não foi possível conectar ao servidor.' 
            });
        } finally {
            this.setLoading(false, this.registerButton);
        }
    }


    async login() {
        this.setLoading(true, this.loginButton);

        const data = {
            email: this.email,
            senha: this.password
        };

        try {

            const response = await fetch(this.API_LOGIN, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(data)
            });

            if (!response.ok) {
                let errorMsg = `Erro HTTP! Status: ${response.status}`;
                try {
                    const errorData = await response.json();
                    errorMsg = errorData.message || JSON.stringify(errorData);
                } catch (e) {
                    errorMsg = await response.text() || errorMsg;
                }
                throw new Error(errorMsg);
            }


            const responseData = await response.json();
            console.log('Success:', responseData);
            
            if (responseData.token) {
                localStorageManager.saveToken(responseData.token);
                window.location.href = 'index.html';
            } else {
                throw new Error('Login bem-sucedido, mas nenhum token foi recebido.');
            }

        } catch (error) {
            console.error('Falha no Login:', error);
            this.showLoginError(error.message || 'Não foi possível conectar ao servidor.');
        
        } finally {
            this.setLoading(false, this.loginButton);
        }
    }


    setLoading(isLoading, button) {
        if (!button) return;

        if (isLoading) {
            button.disabled = true;
            button.innerHTML = '<span class="spinner-border spinner-border-sm" role="status" aria-hidden="true"></span> Carregando...';
            

            if (this.errorElement) {
                this.errorElement.textContent = '';
                this.errorElement.style.display = 'none';
            }
        } else {
            button.disabled = false;
            button.innerHTML = button.originalText;
        }
    }

    showLoginError(message) {
        if (!this.errorElement) return;
        
        const alertBox = document.getElementById('alert-error'); 
        
        this.errorElement.textContent = message;
        if (alertBox) {
            alertBox.classList.remove('d-none');
        }
    }
}

class localStorageManager {
    constructor() {} 

    static saveToken(token) {
        localStorage.setItem('jwt_token', token);
    }
    static getToken() {
        return localStorage.getItem('jwt_token');
    }
    static removeToken() {
        localStorage.removeItem('jwt_token');
    }   
    static isLoggedIn() {
        return !!this.getToken();
    }
}