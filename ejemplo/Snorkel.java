package parqueEcologico;

import java.util.concurrent.Semaphore;
/**
 *
 * @author Nicolas Comeron
 */
public class Snorkel {
    private Semaphore equipoSnorkel;
    private Semaphore salvavidas;
    private Semaphore patasDeRana;
    private Semaphore mutexVisitante;
    private Semaphore mutexAsistente;
    private Semaphore mutexActividadSnorkel;
    private int horario;
    private int contAsistentesEnEspera;
    private boolean apertura;

    public Snorkel() {
        //Valores aleatorios comprendidos dentro de un rango de [10, 20] para el equipamiento  
        int x = (int) (Math.random() * 11) + 10;
        int y = (int) (Math.random() * 11) + 10;
        int z = (int) (Math.random() * 11) + 10;
        System.out.println("cantSnorkel: "+x+" - cantSalvavidas: "+y+" - cantPatas: "+z);
        this.equipoSnorkel = new Semaphore(x, true);
        this.salvavidas = new Semaphore(y, true);
        this.patasDeRana = new Semaphore(z, true);
        this.mutexAsistente = new Semaphore(2, true); /* Inicializado en 2 (cant de Asistentes), este mutex permite hacer esperar
                                                         a los Visitantes en caso que ambos Asistentes se encuentren atendiendo a otros */
        this.mutexVisitante = new Semaphore(0, true); /* Este mutex notifica (es adquirido) por alguno de los Asistentes. 
                                                         Indica que un Visitante ha ingresado al area de la actividad de Snorkel */
        this.mutexActividadSnorkel = new Semaphore(0, true); //Permite que el Visitante continue con la actividad
        this.contAsistentesEnEspera = 0; //Cuenta la cantidad de Asistentes dormidos.
        
        //Instanciacion de los 2 Asistentes (Hilos) que cumplen la funcion de hacer la entrega de equipamiento a los Visitantes
        Thread[] asistentes = new Thread[2];
        for (int i = 0; i < 2; i++) {
            asistentes[i] = new Thread((new Asistente(this, i + 1)));
            asistentes[i].start();
        }
    }

    public void disfrutarSnorkel(int numVisitante) {
        try {
            System.out.println("\t\t\t\t --➊ El Visitante " + numVisitante + " desea practicar Snorkel.");
            mutexAsistente.acquire(); //Cuando adquiere el permiso, significa que un Asistente se encuentra disponible
                System.out.println("\t\t\t\t --➊ El Visitante " + numVisitante + " esta siendo atendido por un Asistente");
                mutexVisitante.release(); //Libera un permiso que es consumido por algun Asistente
                if (contAsistentesEnEspera == 2) /* Significa que si los 2 Asistentes se encuentran durmiendo, se notifique su llegada. 
                                  En caso contrario, existe, al menos, 1 Asistente disponible y no es necesario avisar el arribo. */
                    this.notificarAsistente();
                mutexActividadSnorkel.acquire(); //Y se queda a la espera de que el Asistente habilite el equipo
                    if (apertura) { //Si se encuentra en horario: simula la actividad Snorkel, sino el Visitante se retira
                        System.out.println("\t\t\t\t --➊ El Visitante " + numVisitante + " comienza a practicar Snorkel...");
                            Thread.sleep((int) (Math.random() * 2000 )+ 6000);
                        System.out.println("\t\t\t\t --➊ El Visitante " + numVisitante + " finalizo la practica de Snorkel.");
                        equipoSnorkel.release(); //Al finalizar, devuelve cada uno de los elementos
                        salvavidas.release();
                        patasDeRana.release();
                        System.out.println("\t\t\t\t --➊ El Visitante " + numVisitante + " devolvio el equipamiento y abandona la Actividad Snorkel.");
                    } 
                    else { //En caso que el parque esté por cerrar, suelta los elementos
                        System.out.println("\t\t\t\t --➊ No hay tiempo suficiente para realizar Snorkel. El Visitante " + numVisitante + " abandona el lugar.");
                        equipoSnorkel.release();
                        salvavidas.release();
                        patasDeRana.release();
                    }
        } catch (InterruptedException ex) {
        }
    }
    
    private synchronized void notificarAsistente() {
        this.notify();
    }
    
    public boolean atenderVisitante() {
        return (mutexVisitante.tryAcquire());
    }
    
    public void finalizarAtenderVisitante() {
        mutexAsistente.release();
    }
    
    public synchronized void esperarVisitante(int numAsistente) {
        System.out.println("\t\t\t\t --➊ El Asistente "+numAsistente+" se queda a la espera de nuevos Visitantes.");
        contAsistentesEnEspera++;
        while ((mutexVisitante.availablePermits()) == 0) {
            try {
                this.wait();
            } catch (InterruptedException ex) {}
        }
        System.out.println("\t\t\t\t --➊ El Asistente "+numAsistente+" fue notificado sobre la llegada de un nuevo Visitante.");
        contAsistentesEnEspera--;
    }
    
    public void entregarEquipo(int numAsistente) {
        /* Este metodo es invocado por el hilo Asistente, el cual realiza la entrega de los elementos necesarios
           para disfrutar de la actividad Snorkel                                                             */ 
        try {
            /* Va adquiriendo las partes, en caso de que alguna se encuentre en uso y no haya mas stock, debera 
               esperar hasta que alguno de los Visitantes complete la actividad y retorne las mismas         */ 
            equipoSnorkel.acquire();
            salvavidas.acquire();
            patasDeRana.acquire();
            if (apertura) { //Comprueba estar en horario y simula la entrega (en caso positivo)
                System.out.println("\t\t\t\t --➊ Entregando equipo de snorkel al Visitante...");
                    Thread.sleep((int) (Math.random() * 2000 )+ 1000);
                System.out.println("\t\t\t\t --➊ El Asistente " + numAsistente + " realizo la entrega del equipo correctamente.");
            }
            mutexActividadSnorkel.release(); //Notifica al Visitante para continuar la ejecucion
        } catch (InterruptedException ex) {
        }
    }

    public synchronized void actualizarHoraSnorkel(int hora) {
        /* Cada vez que la hora cambie, es actualizada: si es horario de apertura (9am), se activa la var booleana
           y, ademas, se despierta a los Asistentes que se quedan dormidos mientras no sea un horario laboral. 
           Mientras que si se cumplen las 18, la var booleana pasa a false                                      */
        this.horario = hora;
        if (horario == 9) {
            apertura = true;
            this.notifyAll();
        } 
        else {
            if (horario == 18) 
                apertura = false;
        }
    }

    public synchronized void verificarHorarioDeApertura() {
        /* Este metodo es utilizado por los Hilos Asistentes para comprobar el horario actual, y si deben trabajar
           o quedarse dormidos hasta ser despertados nuevamente                                                 */
        while (!apertura) {
            try {
                this.wait();
            } catch (InterruptedException ex) {
            }
        }
    }
}
