import org.stellar.sdk.*;
import org.stellar.sdk.responses.AccountResponse;

import java.io.IOException;
import java.net.URL;
import java.util.Scanner;

public class Main
{
    static Server server = new Server("https://horizon-testnet.stellar.org");

    public static void main(String[] args)
    {
        boolean run = true;
        KeyPair pair = null;

        System.out.println("Pick from the options:");
        System.out.println("0) Exit \n1) Generate keys\n2) Fill account with Lumen\n3) Get account info\n");

        Scanner reader = new Scanner(System.in);

        do
        {
            switch (reader.nextInt())
            {
                case 0:
                    System.out.println("Exiting...");
                    run = false;
                    break;

                case 1:
                    pair = KeyPair.random();

                    System.out.println("Generated keys: \npublic: " + pair.getAccountId() + "\nsecret: " + new String(pair.getSecretSeed()));
                    break;

                case 2:
                    fillAccount(pair);
                    //fillAccount();
                    break;

                case 3:
                    accountInfo(pair);
                    break;

                case 4:

                    /*
                        Account 1: issuing account:
                        public: GC7YXR3VSFXRBWXPL5QJAY7E67A7622JEA7D4GVYZUYDHOX7JKXUEPMA
                        secret: SDLSWTBGEFJYV6C3SVBQVUBIEGNPS2OEELAK5TH6SMLNZTW7MOMYZPDU

                        Account 2: receiving account:
                        public: GCEKJVUUY4GFUN3JPDPIOJ6QEQOGJ7PP4IP5MYVUDPVCH33B3DNQTW7R
                        secret: SCJKAGDXNKJ3ER25F4WHMGWXA2CT337U5HPDWYS4KCATP6E6RXGFA55A

                        Account 3: test account:
                        public: GD3H3PFHVCPO2YTZAMF2CEP4M7HXYDXX2Z2MQWHUPAJLS5MF56AVCT7N
                        secret:  SB7EHAOXGEI5ZXMROFLDVMAALXJI53ZAJVVW3GPDUJY3QG7TZ7XDQEHT

                        ABOUT THE TRUSTLINE:
                        - trustline defines the maximum amount of a tokan that an account can have
                        - the maximum amount of that token is defined by the issuer of the token
                        - in the case of squarewave I believe that the trustline amount should equal the total amount
                          of payment that has existed throughout the platform ?
                          -> kind of stupid because then you would need to keep track of the amount of tokens issued
                          and how much of it was exchange for fiat EURO and then burned from client wallets.
                          -> else trustline amount could be an int64 (max size possible).

                        HOW IT WORKS:
                        -> primary account creates trustline with another account
                        -> primary account sends the token to that account
                        -> only "trustlined" accounts can trade the specific token, else it doesn't show up on the exchange


                        public:     GCOJOH266BLDOMO7AIGOH56OXV35P4XVOYHXAOHLC4VIH7CBYRZ4IWVB
                        private:    SAYLRZXKAR5H4HJYONJ4CLBBHNJB67YKF3DUXPMUXRZ46CK3NQ2SZW2W
                     */

                    Network.useTestNetwork() ;

                    // Keys for accounts to issue and receive the new asset
                    KeyPair issuingKeys = KeyPair.fromSecretSeed("SDLSWTBGEFJYV6C3SVBQVUBIEGNPS2OEELAK5TH6SMLNZTW7MOMYZPDU");

                    KeyPair receivingKeys = KeyPair.fromSecretSeed("SB7EHAOXGEI5ZXMROFLDVMAALXJI53ZAJVVW3GPDUJY3QG7TZ7XDQEHT");

                    // Create an object to represent the new asset
                    Asset astroDollar = Asset.createNonNativeAsset("IodaDollar ", issuingKeys);

                    // First, the receiving account must trust the asset
                    AccountResponse receiving = null;

                    try
                    {
                        receiving = server.accounts().account(receivingKeys);
                    }
                    catch (IOException e)
                    {
                        e.printStackTrace();
                    }

                    // The `ChangeTrust` operation creates (or alters) a trustline
                    // The second parameter limits the amount the account can hold

                    if (receiving != null)
                    {
                        // max 64 int 18446744073709551615
                        Transaction allowAstroDollars = new Transaction.Builder(receiving).addOperation(new ChangeTrustOperation.Builder(astroDollar, "2000").build()).build();

                        allowAstroDollars.sign(receivingKeys);

                        try
                        {
                            server.submitTransaction(allowAstroDollars);
                        }
                        catch (IOException e)
                        {
                            e.printStackTrace();
                        }
                    }
                    else
                    {
                        System.out.println("Receiving account is empty");
                    }

                    System.out.println("Receiving account DONE");

                    // Second, the issuing account actually sends a payment using the asset
                    AccountResponse issuing = null;

                    try
                    {
                        issuing = server.accounts().account(issuingKeys);
                    }
                    catch (IOException e)
                    {
                        e.printStackTrace();
                    }

                    Transaction sendAstroDollars = new Transaction.Builder(issuing).addOperation(new PaymentOperation.Builder(receivingKeys, astroDollar, "130").build()).build();

                    sendAstroDollars.sign(issuingKeys);

                    try
                    {
                        server.submitTransaction(sendAstroDollars);
                    }
                    catch (IOException e)
                    {
                        e.printStackTrace();
                    }

                    System.out.println("Sending account DONE");

                    break;

                default:
                    System.out.println("Wrong input, try again\n");
                    break;
            }
        } while (run);
        reader.close();
    }

    private static void fillAccount(KeyPair pair)
    //private static void fillAccount()
    {
        String friendBot = String.format("https://friendbot.stellar.org/?addr=%s", pair.getAccountId());

        System.out.println("Processing payment...");
        try
        {
            new URL(friendBot).openStream();
            /*new URL(String.format("https://friendbot.stellar.org/?addr=%s", "GC7YXR3VSFXRBWXPL5QJAY7E67A7622JEA7D4GVYZUYDHOX7JKXUEPMA")).openStream();
            new URL(String.format("https://friendbot.stellar.org/?addr=%s", "GCEKJVUUY4GFUN3JPDPIOJ6QEQOGJ7PP4IP5MYVUDPVCH33B3DNQTW7R")).openStream();
*/
            System.out.println("Payment received!");
        } catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    private static void accountInfo(KeyPair pair)
    {
        AccountResponse account = null;

        try
        {
            account = server.accounts().account(pair);
        } catch (IOException e)
        {
            System.out.println("here");
            e.printStackTrace();
        }

        System.out.println("Balances for account: " + pair.getAccountId());

        if (account != null)
        {
            for (AccountResponse.Balance balance : account.getBalances())
            {
                System.out.println("Type: " + balance.getAssetType() +
                        " Code: " + balance.getAssetCode() +
                        " Balance: " + balance.getBalance());
            }
        } else
        {
            System.out.println("Account empty, something is wrong!");
        }
    }
}
