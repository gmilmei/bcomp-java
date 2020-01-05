package gemi.bcomp.vm;

import static gemi.bcomp.scanner.Scanner.EOT;
import static gemi.bcomp.utilities.Utilities.string2words;
import static gemi.bcomp.vm.VMResult.OK;
import static gemi.bcomp.vm.VMResult.UNDEFINED_SYSTEM_CALL;
import static gemi.bcomp.vm.VMResult.UNSUPPORTED_SYSTEM_CALL;

import java.io.File;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.RandomAccessFile;
import java.lang.ProcessBuilder.Redirect;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementation of most library functions via system calls.
 */
public class SystemCalls {

    private VM vm;
    private String cwd = System.getProperty("user.dir");
    private RandomAccessFile[] openfiles = new RandomAccessFile[20];
    
    public PrintStream stdout = System.out;
    public InputStream stdin = System.in;
    private List<String> argv = null;
    
    public SystemCalls(VM vm) {
        this.vm = vm;
    }
    
    public void argv(List<String> argv) {
        this.argv = argv;
    }
    
    public VMResult syscall(int sys) {
        switch (sys) {
        case 1:
            // c = char(string, i)
            schar();
            return OK;
        case 2:
            // error = chdir(string)
            chdir();
            return OK;
        case 3:
            // error = chmod(string, mode)
            return UNSUPPORTED_SYSTEM_CALL;
        case 4:
            // error = chown(string, owner)
            return UNSUPPORTED_SYSTEM_CALL;
        case 5:
            // error = close(file)
            close();
            return OK;
        case 6:
            // error = creat(string, mode)
            creat();
            return OK;
        case 7:
            // ctime(time, date)
            ctime();
            return OK;
        case 8:
            // execl(string, arg0, arg1, ..., 0)
            execl();
            return OK;
        case 9:
            // execv(string, argv, count)
            execv();
            return OK;
        case 11:
            // error = fork()
            return UNSUPPORTED_SYSTEM_CALL;
        case 12:
            // error = fstat(file, status)
            return UNSUPPORTED_SYSTEM_CALL;
        case 13:
            // char = getchar()
            getchar();
            return OK;
        case 14:
            // id = getuid()
            return UNSUPPORTED_SYSTEM_CALL;
        case 15:
            // error = gtty(file, ttystat)
            return UNSUPPORTED_SYSTEM_CALL;
        case 16:
            // lchar(string, i, char)
            lchar();
            return OK;
        case 17:
            // error = link(string1, string2)
            return UNSUPPORTED_SYSTEM_CALL;
        case 18:
            // error = mkdir(string, mode)
            mkdir();
            return OK;
        case 19:
            // file = open(file, mode)
            open();
            return OK;
        case 20:
            // printf(format, arg1, ...)
            printf();
            return OK;
        case 21:
            // printn(number, base);
            printn(arg(1), arg(2));
            return OK;
        case 22:
            // putchar(char)
            outchar(stdout, arg(1));
            return OK;
        case 23:
            // nread = read(file, buffer, count)
            read();
            return OK;
        case 24:
            // error = seek(file, offset, pointer)
            seek();
            return OK;
        case 25:
            // error = setuid(id)
            return UNSUPPORTED_SYSTEM_CALL;
        case 26:
            // error = stat(string, status)
            return UNSUPPORTED_SYSTEM_CALL;
        case 27:
            // error = stty(file, ttystat)
            return UNSUPPORTED_SYSTEM_CALL;
        case 28:
            // time(timev)
            time();
            return OK;
        case 29:
            // error = unlink(string)
            unlink();
            return OK;
        case 30:
            // error = wait()
            return UNSUPPORTED_SYSTEM_CALL;
        case 31:
            // nwrite = write(file, buffer, count)
            write();
            return OK;
        case 50:
            // set argv variable
            setArgv();
            return OK;
        default:
            return UNDEFINED_SYSTEM_CALL;
        }
    }
    
    private void schar() {
        int s = arg(1);
        int i = arg(2);
        s += i/4;
        ret((mem(s) >> (8*(i%4)))&0xFF);
    }
    
    private void chdir() {
        String s = string(arg(1));
        if (!s.startsWith("/")) s = cwd+'/'+s;
        File dir = new File(s);
        if (dir.exists() && dir.isDirectory() && dir.canExecute()) {
            cwd = s;
            ret(0);
        }
        else {
            ret(-1);
        }
    }
    
    private void close() {
        int fd = arg(1)-3;
        RandomAccessFile file = getFile(fd);
        if (file != null) {
            openfiles[fd] = null;
            try {
                file.close();
            } catch (Exception e) {
                ret(-1);
                return;
            }
            ret(0);
            return;
        }
        ret(-1);
    }

    private void execl() {
        List<String> command = new ArrayList<>();
        command.add(string(arg(1)));
        int n = 2;
        while (arg(n) != 0) {
            command.add(string(arg(n)));
            n++;
        }
        try {
            ProcessBuilder builder = new ProcessBuilder(command);
            builder.redirectOutput(Redirect.INHERIT);
            builder.redirectError(Redirect.INHERIT);
            Process p = builder.start();
            p.waitFor();
            System.exit(p.waitFor());
        } catch (Exception e) {}
        ret(-1);
    }

    private void execv() {
        List<String> command = new ArrayList<>();
        command.add(string(arg(1)));
        int argv = arg(2);
        int count = arg(3);
        for (int i = 0; i < count; i++) {
            command.add(string(mem(argv)));     
            argv++;
        }
        try {
            ProcessBuilder builder = new ProcessBuilder(command);
            builder.redirectOutput(Redirect.INHERIT);
            builder.redirectError(Redirect.INHERIT);
            Process p = builder.start();
            p.waitFor();
            System.exit(p.waitFor());
        } catch (Exception e) {}
        ret(-1);
    }
    
    private void creat() {
        String filename = string(arg(1));
        int mode = arg(2);        
        try {
            int fd = -1;
            for (int i = 0; i < openfiles.length; i++) {
                if (openfiles[i] == null) {
                    fd = i;
                    break;
                }
            }
            if (fd < 0) ret(-1);
            @SuppressWarnings("resource")
            RandomAccessFile file = new RandomAccessFile(filename, "rw");
            openfiles[fd] = file;
            ret(fd+3);
        } catch (Exception e) {
            ret(-1);
        }
    }
    
    private void ctime() {
        // TODO
    }
    
    private void getchar() {
        try {
            int ch = stdin.read();
            if (ch == -1) ch = EOT;
            ret(ch);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void lchar() {
        int i = arg(2);
        int s = arg(1) + i/4;
        int c = (arg(3)&0xFF) << (8*(i%4));
        int m = ~(0xFF << (8*(i%4)));
        mem(s, (mem(s)&m)|c);
    }

    private void mkdir() {
        String dir = string(arg(1));
        int mode = arg(2);
        // TODO
        if (!dir.startsWith("/")) dir = cwd+'/'+dir;
        File file = new File(dir);
        if (file.mkdirs())
            ret(0);
        else     
            ret(-1);
    }
    
    private void open() {
        String filename = string(arg(1));
        int mode = arg(2);        
        try {
            String m = "r";
            if (mode > 0) m = "rw";
            int fd = -1;
            for (int i = 0; i < openfiles.length; i++) {
                if (openfiles[i] == null) {
                    fd = i;
                    break;
                }
            }
            if (fd < 0) ret(-1);
            @SuppressWarnings("resource")
            RandomAccessFile file = new RandomAccessFile(filename, m);
            openfiles[fd] = file;
            ret(fd+3);
        } catch (Exception e) {
            ret(-1);
        }
    }
    
    private void printf() {
        int an = 1;
        int format = arg(an++);
        int word = mem(format);
        int n = 0;
        boolean f = false; 
        while (true) {
            int ch = (word >> (8*n))&0xFF;
            if (ch == EOT) break;
            if (f) {
                if (ch == 'd') {
                    stdout.print(arg(an++));
                }
                else if (ch == 'o') {
                    stdout.format("%o", arg(an++));
                }
                else if (ch == 'x') {
                    stdout.format("%x", arg(an++));
                }
                else if (ch == 'c') {
                    outchar(stdout, arg(an++));
                }
                else if (ch == 's') {
                    outstring(stdout, arg(an++));
                }
                else {
                    stdout.write(ch);
                }
                f = false;
            }
            else if (ch == '%') {
                f = true;
            }
            else {
                stdout.write(ch);
            }
            n++;
            if (n == 4) {
                format++;
                word = mem(format);
                n = 0;
            }
        }
        stdout.flush();
    }
    
    private void printn(int n, int b) {
        if (b < 2 || b > 10) return;
        int a = n/b;
        if (a != 0) printn(a, b);
        outchar(stdout, n%b+'0');
    }
    
    private void read() {
        int fd = arg(1)-3;
        int buf = arg(2);
        int count = arg(3);
        RandomAccessFile file = getFile(fd);
        if (file == null) {
            ret(-1);
            return;
        }
        // TODO
    }
    
    private void seek() {
        int fd = arg(1)-3;
        int offset = arg(2);
        int pointer = arg(3);
        RandomAccessFile file = getFile(fd);
        if (file == null) {
            ret(-1);
            return;
        }
        try {
            if (pointer == 0) {
                file.seek(offset);
            }
            else if (pointer == 1) {
                file.seek(offset+file.getFilePointer());
            }
            else if (pointer == 2) {
                file.seek(offset+file.length());
            }
            ret(0);
        } catch (Exception e) {
            ret(-1);
        }
    }
    
    private void time() {
        long t = System.currentTimeMillis();
        long t60 = t*60/1000;
        int timev = arg(1);
        mem(timev, (int)(t60&0xFFFFFFFF));
        mem(timev+1, (int)((t60>>32)&0xFFFFFFFF));
    }
    
    private void unlink() {
        String filename = string(arg(1));
        File file = new File(filename);
        if (file.delete())
            ret(0);
        else
            ret(-1);
    }
    
    private void write() {
        int fd = arg(1)-3;
        int buf = arg(2);
        int count = arg(3);
        RandomAccessFile file = getFile(fd);
        if (file == null) {
            ret(-1);
            return;
        }
        try {
            int n = 0;
            while (n < count) {
                int v = mem(buf);
                int i = 0;
                while (n < count && i < 4) {
                    file.writeByte(v&0xFF);
                    v = v >> 8;
                i++;
                n++;
                }
                buf++;
            }
            ret(n);
        } catch (Exception e) {}
        ret(-1);
    }

    private void outchar(PrintStream out, int c) {
        while (c != 0) {
            out.write(c&0xFF);
            c >>= 8;
        }
        out.flush();
    }
    
    private void outstring(PrintStream out, int s) {
        int word = mem(s);
        int n = 0;
        while (true) {
            int ch = (word >> (8*n))&0xFF;
            if (ch == EOT) break;
            out.write(ch);
            n++;
            if (n == 4) {
                s++;
                word = mem(s);
                n = 0;
            }
        }
        out.flush();
    }
    
    private String string(int s) {
        StringBuilder buf = new StringBuilder();
        int word = mem(s);
        int n = 0;
        while (true) {
            int ch = (word >> (8*n))&0xFF;
            if (ch == EOT) break;
            buf.append((char)ch);
            n++;
            if (n == 4) {
                s++;
                word = mem(s);
                n = 0;
            }
        }
        return buf.toString();
    }
    
    private RandomAccessFile getFile(int fd) {
        if (fd >= 0 && fd < openfiles.length) {
            RandomAccessFile file = openfiles[fd];
            if (file != null) return file;
        }
        return null;
    }

    protected int nargs() {
        return arg(0);
    }
    
    private final int mem(int adr) {
        return vm.mem(adr);
    }

    private final void mem(int adr, int value) {
        vm.mem(adr, value);
    }

    private final int arg(int n) {
        return mem(vm.regs[VM.FP]+2+n);
    }
    
    private final void ret(int n) {
        vm.regs[VM.acc] = n;
    }
    
    private void setArgv() {
        int av = mem(arg(1));
        if (argv == null) {
            mem(av, 0);
        }
        else {
            int n = Math.min(20, argv.size());
            mem(av, n);
            for (String arg : argv) {
                av++;
                int[] words = string2words(arg);
                mem(av, vm.data);
                for (int word : words) {
                    mem(vm.data, word);
                    vm.data++;
                }
            }
        }
    }
}
